package io.github.imaping.token.redis.registry;

import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.api.registry.AbstractTokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class RedisTokenRegistry extends AbstractTokenRegistry {

    private static final long DEFAULT_SCAN_COUNT = 500L;
    private static final String TOKEN_DATA_PREFIX = "imaping.token:data:";
    private static final String TOKEN_DATA_PATTERN = TOKEN_DATA_PREFIX + '*';
    private static final String TOKEN_OWNER_INDEX_PREFIX = "imaping.token:index:token:";
    private static final String TOKEN_OWNER_INDEX_PATTERN = TOKEN_OWNER_INDEX_PREFIX + '*';
    private static final String PRINCIPAL_TOKENS_PREFIX = "imaping.token:principal:";
    private static final String PRINCIPAL_TOKENS_PATTERN = PRINCIPAL_TOKENS_PREFIX + '*';

    private final TokenRedisTemplate<String, Object> client;

    private static Long getTimeout(final Token token) {
        long ttl = defaultIfNegative(token.getExpirationPolicy().getTimeToLive());
        long idle = defaultIfNegative(token.getExpirationPolicy().getTimeToIdle());
        long timeout = ttl > 0 && idle > 0 ? Math.min(ttl, idle) : Math.max(ttl, idle);
        if (timeout > Integer.MAX_VALUE) {
            return (long) Integer.MAX_VALUE;
        } else if (timeout <= 0) {
            return 0L;
        }
        return timeout;
    }

    private static long defaultIfNegative(final Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private static String getTokenRedisKey(final String tokenId) {
        return TOKEN_DATA_PREFIX + StringUtils.trimToEmpty(tokenId);
    }

    private static String getTokenOwnerIndexKey(final String tokenId) {
        return TOKEN_OWNER_INDEX_PREFIX + StringUtils.trimToEmpty(tokenId);
    }

    private static String getPrincipalTokensKey(final String principalId) {
        return PRINCIPAL_TOKENS_PREFIX + StringUtils.trimToEmpty(principalId);
    }

    @Override
    public long deleteAll() {
        val tokenKeys = getKeysStream(TOKEN_DATA_PATTERN).collect(Collectors.toCollection(LinkedHashSet::new));
        val indexKeys = getKeysStream(TOKEN_OWNER_INDEX_PATTERN).collect(Collectors.toCollection(LinkedHashSet::new));
        val principalKeys = getKeysStream(PRINCIPAL_TOKENS_PATTERN).collect(Collectors.toCollection(LinkedHashSet::new));
        deleteKeys(indexKeys);
        deleteKeys(principalKeys);
        deleteKeys(tokenKeys);
        return tokenKeys.size();
    }

    @Override
    public long deleteSingleToken(final String tokenId) {
        if (StringUtils.isBlank(tokenId)) {
            return 0;
        }
        final String normalizedTokenId = tokenId.trim();
        final String tokenKey = getTokenRedisKey(normalizedTokenId);
        final boolean tokenExisted = BooleanUtils.isTrue(client.hasKey(tokenKey));
        cleanupTokenIndexes(normalizedTokenId, resolvePrincipalId(normalizedTokenId));
        return tokenExisted ? 1 : 0;
    }

    @Override
    public void addTokentInternal(final Token token) {
        try {
            log.debug("Adding token [{}]", token);
            storeToken(token);
        } catch (final Exception e) {
            log.error("Failed to add [{}]", token, e);
        }
    }

    @Override
    public Token getToken(final String tokenId, final Predicate<Token> predicate) {
        try {
            val storedValue = client.boundValueOps(getTokenRedisKey(tokenId)).get();
            if (!(storedValue instanceof Token token)) {
                cleanupTokenIndexes(tokenId, resolvePrincipalId(tokenId));
                return null;
            }
            return predicate.test(token) ? token : null;
        } catch (final Exception e) {
            log.error("Failed fetching [{}]", tokenId, e);
        }
        return null;
    }

    @Override
    public Collection<? extends Token> getTokens() {
        try (val tokensStream = stream()) {
            return tokensStream.collect(Collectors.toSet());
        }
    }

    @Override
    public Stream<? extends Token> stream() {
        return getKeysStream(TOKEN_DATA_PATTERN)
                .map(this::extractTokenId)
                .map(this::getToken)
                .filter(Objects::nonNull);
    }

    @Override
    public Token updateToken(final Token token) {
        try {
            log.debug("Updating token [{}]", token);
            return storeToken(token);
        } catch (final Exception e) {
            log.error("Failed to update [{}]", token, e);
        }
        return null;
    }

    @Override
    public Stream<? extends Token> getSessionsFor(final String principalId) {
        val members = client.opsForSet().members(getPrincipalTokensKey(principalId));
        if (members == null || members.isEmpty()) {
            return Stream.empty();
        }
        return members.stream()
                .map(String::valueOf)
                .map(tokenId -> toSessionToken(principalId, tokenId))
                .filter(Objects::nonNull);
    }

    private Token storeToken(final Token token) {
        final String tokenId = token.getId();
        final String principalId = getPrincipalIdFrom(token);
        final String previousPrincipalId = resolvePrincipalId(tokenId);
        if (StringUtils.isNotBlank(previousPrincipalId) && !StringUtils.equals(previousPrincipalId, principalId)) {
            cleanupPrincipalSession(previousPrincipalId, tokenId);
        }
        final long timeout = getTimeout(token);
        storeValue(getTokenRedisKey(tokenId), token, timeout);
        if (StringUtils.isNotBlank(principalId)) {
            storeValue(getTokenOwnerIndexKey(tokenId), principalId, timeout);
            client.opsForSet().add(getPrincipalTokensKey(principalId), tokenId);
        } else {
            client.delete(getTokenOwnerIndexKey(tokenId));
        }
        return token;
    }

    private void storeValue(final String key, final Object value, final long timeout) {
        if (timeout <= 0) {
            client.opsForValue().set(key, value);
            return;
        }
        client.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    private String resolvePrincipalId(final String tokenId) {
        if (StringUtils.isBlank(tokenId)) {
            return StringUtils.EMPTY;
        }
        final Object indexedValue = client.boundValueOps(getTokenOwnerIndexKey(tokenId.trim())).get();
        if (indexedValue instanceof String indexedPrincipalId) {
            return indexedPrincipalId;
        }
        final Object tokenValue = client.boundValueOps(getTokenRedisKey(tokenId.trim())).get();
        if (tokenValue instanceof Token token) {
            return getPrincipalIdFrom(token);
        }
        return StringUtils.EMPTY;
    }

    private Token toSessionToken(final String principalId, final String tokenId) {
        final Token token = getToken(tokenId);
        if (token == null || !StringUtils.equals(principalId, getPrincipalIdFrom(token))) {
            cleanupPrincipalSession(principalId, tokenId);
            return null;
        }
        return token;
    }

    private void cleanupTokenIndexes(final String tokenId, final String principalId) {
        deleteKeys(Set.of(getTokenRedisKey(tokenId), getTokenOwnerIndexKey(tokenId)));
        cleanupPrincipalSession(principalId, tokenId);
    }

    private void cleanupPrincipalSession(final String principalId, final String tokenId) {
        if (StringUtils.isBlank(principalId)) {
            return;
        }
        final String principalKey = getPrincipalTokensKey(principalId);
        client.opsForSet().remove(principalKey, tokenId);
        Long size = client.opsForSet().size(principalKey);
        if (size != null && size == 0) {
            client.delete(principalKey);
        }
    }

    private void deleteKeys(final Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        client.delete(keys);
    }

    private String extractTokenId(final String tokenKey) {
        return StringUtils.removeStart(tokenKey, TOKEN_DATA_PREFIX);
    }

    private Stream<String> getKeysStream(final String pattern) {
        return client.scan(pattern, DEFAULT_SCAN_COUNT);
    }
}

