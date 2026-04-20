package io.github.imaping.token.api.registry;

import io.github.imaping.token.api.authentication.AuthenticationAwareToken;
import io.github.imaping.token.api.exception.ConcurrentSessionControlException;
import io.github.imaping.token.api.lock.LockRepository;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.configuration.model.token.ConcurrentSessionOverflowStrategy;
import io.github.imaping.token.configuration.model.token.ConcurrentSessionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 在 TokenRegistry 层统一执行并发会话控制。
 */
@RequiredArgsConstructor
@Slf4j
public class ConcurrentSessionControlTokenRegistry implements TokenRegistry {

    private static final Comparator<Token> SESSION_ORDER = Comparator
            .comparing(Token::getCreationTime)
            .thenComparing(Token::getId);

    private final TokenRegistry delegate;
    private final LockRepository lockRepository;
    private final ConcurrentSessionProperties properties;

    @Override
    public void addToken(final Token token) throws Exception {
        if (token == null || !shouldControlToken(token)) {
            delegate.addToken(token);
            return;
        }

        final int maxSessions = properties.getMaxSessions();
        if (maxSessions < 1) {
            throw new ConcurrentSessionControlException("maxSessions must be greater than 0 when concurrent session control is enabled");
        }

        final String principalId = getPrincipalId(token);
        if (!StringUtils.hasText(principalId)) {
            log.debug("Skipping concurrent session control because principal id is blank for token [{}]", token.getId());
            delegate.addToken(token);
            return;
        }

        final String lockKey = "concurrent-session:" + principalId;
        lockRepository.execute(lockKey, () -> {
            try {
                enforceLimit(principalId, maxSessions);
                delegate.addToken(token);
                return token;
            } catch (Exception e) {
                throw new ConcurrentSessionControlException("Failed to add token with concurrent session control", e);
            }
        }).orElseThrow(() -> new ConcurrentSessionControlException(
                "Failed to acquire lock for concurrent session control of principal [" + principalId + "]"));
    }

    private void enforceLimit(final String principalId, final int maxSessions) throws Exception {
        try (Stream<? extends Token> sessions = delegate.getTokens(controlledSessionPredicate(principalId))) {
            final List<Token> activeSessions = sessions
                    .sorted(SESSION_ORDER)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            if (activeSessions.size() < maxSessions) {
                return;
            }
            applyOverflowStrategy(principalId, activeSessions, maxSessions);
        }
    }

    private Predicate<Token> controlledSessionPredicate(final String principalId) {
        return token -> token != null
                && !token.isExpired()
                && shouldControlToken(token)
                && principalId.equals(getPrincipalId(token));
    }

    private void applyOverflowStrategy(final String principalId,
                                       final List<Token> activeSessions,
                                       final int maxSessions) throws Exception {
        if (properties.getOverflowStrategy() == ConcurrentSessionOverflowStrategy.DENY_NEW_LOGIN) {
            throw new ConcurrentSessionControlException(
                    "Principal [" + principalId + "] already has the maximum number of active sessions: " + maxSessions);
        }

        int sessionsToDelete = activeSessions.size() - maxSessions + 1;
        for (Token session : activeSessions) {
            if (sessionsToDelete <= 0) {
                return;
            }
            log.info("Deleting existing session [{}] for principal [{}] due to concurrent session limit [{}]",
                    session.getId(), principalId, maxSessions);
            delegate.deleteToken(session);
            sessionsToDelete--;
        }
    }

    private boolean shouldControlToken(final Token token) {
        return properties.isEnabled()
                && token != null
                && matchesConfiguredType(token.getClass());
    }

    private boolean matchesConfiguredType(final Class<?> tokenType) {
        final List<String> enabledTokenTypes = properties.getEnabledTokenTypes();
        if (enabledTokenTypes == null || enabledTokenTypes.isEmpty()) {
            return false;
        }
        final Set<Class<?>> candidates = new LinkedHashSet<>();
        for (Class<?> current = tokenType; current != null; current = current.getSuperclass()) {
            candidates.add(current);
            candidates.addAll(ClassUtils.getAllInterfacesForClassAsSet(current));
        }
        return enabledTokenTypes.stream()
                .filter(StringUtils::hasText)
                .anyMatch(name -> candidates.stream().anyMatch(type -> matchesTypeName(type, name)));
    }

    private boolean matchesTypeName(final Class<?> type, final String candidate) {
        return candidate.equals(type.getSimpleName())
                || candidate.equals(type.getName())
                || candidate.equals(type.getCanonicalName());
    }

    private String getPrincipalId(final Token token) {
        if (token instanceof AuthenticationAwareToken authenticationAwareToken
                && authenticationAwareToken.getAuthentication() != null
                && authenticationAwareToken.getAuthentication().getPrincipal() != null) {
            return authenticationAwareToken.getAuthentication().getPrincipal().getId();
        }
        return null;
    }

    @Override
    public <T extends Token> T getToken(final String tokenId, final Class<T> clazz) {
        return delegate.getToken(tokenId, clazz);
    }

    @Override
    public Token getToken(final String tokenId) {
        return delegate.getToken(tokenId);
    }

    @Override
    public Token getToken(final String tokenId, final Predicate<Token> predicate) {
        return delegate.getToken(tokenId, predicate);
    }

    @Override
    public int deleteToken(final String tokenId) throws Exception {
        return delegate.deleteToken(tokenId);
    }

    @Override
    public int deleteToken(final Token tokenId) throws Exception {
        return delegate.deleteToken(tokenId);
    }

    @Override
    public long deleteAll() {
        return delegate.deleteAll();
    }

    @Override
    public Collection<? extends Token> getTokens() {
        return delegate.getTokens();
    }

    @Override
    public Stream<? extends Token> getTokens(final Predicate<Token> predicate) {
        return delegate.getTokens(predicate);
    }

    @Override
    public Token updateToken(final Token token) throws Exception {
        return delegate.updateToken(token);
    }

    @Override
    public long sessionCount() {
        return delegate.sessionCount();
    }

    @Override
    public Stream<? extends Token> stream() {
        return delegate.stream();
    }

    @Override
    public long countSessionsFor(final String principalId) {
        return delegate.countSessionsFor(principalId);
    }

    @Override
    public Stream<? extends Token> getSessionsFor(final String principalId) {
        return delegate.getSessionsFor(principalId);
    }
}

