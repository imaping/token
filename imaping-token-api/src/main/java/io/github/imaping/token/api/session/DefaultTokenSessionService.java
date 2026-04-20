package io.github.imaping.token.api.session;

import io.github.imaping.token.api.authentication.AuthenticationAwareToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 默认的 Token 会话管理服务实现。
 */
@RequiredArgsConstructor
public class DefaultTokenSessionService implements TokenSessionService {

    private static final Comparator<TokenSession> SESSION_ORDER = Comparator
            .comparing(TokenSession::getCreationTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(TokenSession::getTokenId, Comparator.nullsLast(String::compareTo));

    private final TokenRegistry tokenRegistry;

    @Override
    public long countSessionsFor(final String principalId) {
        return tokenRegistry.countSessionsFor(principalId);
    }

    @Override
    public List<TokenSession> getSessionsFor(final String principalId) {
        return getSessionsFor(principalId, null);
    }

    @Override
    public List<TokenSession> getSessionsFor(final String principalId, final String currentTokenId) {
        if (!StringUtils.hasText(principalId)) {
            return List.of();
        }
        try (Stream<? extends Token> sessions = tokenRegistry.getSessionsFor(principalId)) {
            return sessions
                    .map(token -> toSession(token, currentTokenId))
                    .filter(Objects::nonNull)
                    .sorted(SESSION_ORDER)
                    .toList();
        }
    }

    @Override
    public TokenSession getSession(final String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return null;
        }
        return toSession(tokenRegistry.getToken(tokenId), null);
    }

    @Override
    public boolean isSessionActive(final String principalId, final String tokenId) {
        return hasOwnedToken(principalId, tokenRegistry.getToken(tokenId));
    }

    @Override
    public int revokeSession(final String principalId, final String tokenId) throws Exception {
        if (!StringUtils.hasText(principalId) || !StringUtils.hasText(tokenId)) {
            return 0;
        }
        Token token = tokenRegistry.getToken(tokenId);
        if (!hasOwnedToken(principalId, token)) {
            return 0;
        }
        return tokenRegistry.deleteToken(tokenId);
    }

    @Override
    public long revokeSessions(final String principalId) throws Exception {
        if (!StringUtils.hasText(principalId)) {
            return 0;
        }
        return revokeMatchingSessions(principalId, null);
    }

    @Override
    public long revokeOtherSessions(final String principalId, final String currentTokenId) throws Exception {
        if (!StringUtils.hasText(principalId)) {
            return 0;
        }
        return revokeMatchingSessions(principalId, currentTokenId);
    }

    private long revokeMatchingSessions(final String principalId, final String excludeTokenId) throws Exception {
        List<String> tokenIds;
        try (Stream<? extends Token> sessions = tokenRegistry.getSessionsFor(principalId)) {
            tokenIds = sessions
                    .map(Token::getId)
                    .filter(StringUtils::hasText)
                    .filter(tokenId -> !StringUtils.hasText(excludeTokenId) || !excludeTokenId.equals(tokenId))
                    .toList();
        }
        long deleted = 0;
        for (String tokenId : tokenIds) {
            deleted += tokenRegistry.deleteToken(tokenId);
        }
        return deleted;
    }

    private boolean hasOwnedToken(final String principalId, final Token token) {
        return StringUtils.hasText(principalId)
                && token != null
                && principalId.equals(extractPrincipalId(token));
    }

    private TokenSession toSession(final Token token, final String currentTokenId) {
        if (token == null) {
            return null;
        }
        return TokenSession.builder()
                .tokenId(token.getId())
                .principalId(extractPrincipalId(token))
                .tokenType(token.getClass().getSimpleName())
                .creationTime(token.getCreationTime())
                .lastTimeUsed(token.getLastTimeUsed())
                .previousTimeUsed(token.getPreviousTimeUsed())
                .countOfUses(token.getCountOfUses())
                .expired(token.isExpired())
                .current(StringUtils.hasText(currentTokenId) && currentTokenId.equals(token.getId()))
                .build();
    }

    private String extractPrincipalId(final Token token) {
        if (token instanceof AuthenticationAwareToken authenticationAwareToken
                && authenticationAwareToken.getAuthentication() != null
                && authenticationAwareToken.getAuthentication().getPrincipal() != null) {
            return authenticationAwareToken.getAuthentication().getPrincipal().getId();
        }
        return null;
    }
}
