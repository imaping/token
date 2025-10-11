package com.imaping.token.redis.registry;

import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.AbstractTokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class RedisTokenRegistry extends AbstractTokenRegistry {

    private static final String TOKEN_PREFIX = "imaping.token:";

    private final TokenRedisTemplate<String, Token> client;

    private static Long getTimeout(final Token token) {
        val ttl = token.getExpirationPolicy().getTimeToLive();
        if (ttl > Integer.MAX_VALUE) {
            return (long) Integer.MAX_VALUE;
        } else if (ttl <= 0) {
            return 0L;
        }
        return ttl;
    }

    private static String getTokenRedisKey(final String tokenId, final String user) {
        return TOKEN_PREFIX
                + StringUtils.defaultIfBlank(tokenId.trim(), "*")
                + ':'
                + StringUtils.defaultIfBlank(user.trim(), "*");
    }

    private static String getPatternTokenRedisKey() {
        return TOKEN_PREFIX + '*';
    }

    @Override
    @SuppressWarnings("java:S2583")
    public long deleteAll() {
        val redisKeys = getKeysStream().collect(Collectors.toSet());
        val size = Objects.requireNonNull(redisKeys).size();
        this.client.delete(redisKeys);
        return size;
    }

    @Override
    public long deleteSingleToken(final String tokenId) {
        String redisKey = getTokenRedisKey(tokenId, StringUtils.EMPTY);
        return getKeysStream(redisKey).mapToInt(id -> BooleanUtils.toBoolean(client.delete(id)) ? 1 : 0).sum();
    }

    @Override
    public void addTokentInternal(final Token token) {
        try {
            log.debug("Adding token [{}]", token);
            String userId = getPrincipalIdFrom(token);
            String redisKey = getTokenRedisKey(token.getId(), userId);
            Long timeout = getTimeout(token);
            if (timeout == 0L) {
                this.client.boundValueOps(redisKey).set(token);
            } else {
                this.client.boundValueOps(redisKey).set(token, timeout, TimeUnit.SECONDS);
            }
        } catch (final Exception e) {
            log.error("Failed to add [{}]", token);
        }
    }

    @Override
    public Token getToken(final String tokenId, final Predicate<Token> predicate) {
        try {
            val redisKey = getTokenRedisKey(tokenId, StringUtils.EMPTY);
            return getKeysStream(redisKey)
                    .map(key -> client.boundValueOps(key).get())
                    .filter(Objects::nonNull)
                    .filter(predicate)
                    .findFirst()
                    .orElse(null);
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
        return getKeysStream()
                .map(redisKey -> {
                    val token = this.client.boundValueOps(redisKey).get();
                    if (token == null) {
                        this.client.delete(redisKey);
                        return null;
                    }
                    return token;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public Token updateToken(final Token token) {
        try {
            log.debug("Updating token [{}]", token);
            val userId = getPrincipalIdFrom(token);
            val redisKey = getTokenRedisKey(token.getId(), userId);
            log.debug("Fetched redis key [{}] for token [{}]", redisKey, token);

            val timeout = getTimeout(token);
            if (timeout == 0L) {
                this.client.boundValueOps(redisKey).set(token);
            } else {
                this.client.boundValueOps(redisKey).set(token, timeout, TimeUnit.SECONDS);
            }
            return token;
        } catch (final Exception e) {
            log.error("Failed to update [{}]", token);
        }
        return null;
    }

    @Override
    public Stream<? extends Token> getSessionsFor(final String principalId) {
        val redisKey = getTokenRedisKey(StringUtils.EMPTY, principalId);
        return getKeysStream(redisKey)
                .map(key -> client.boundValueOps(key).get())
                .filter(Objects::nonNull);
    }

    /**
     * Get a stream of all CAS-related keys from Redis DB.
     *
     * @return stream of all CAS-related keys from Redis DB
     */
    private Stream<String> getKeysStream() {
        return getKeysStream(getPatternTokenRedisKey());
    }

    private Stream<String> getKeysStream(final String key) {
        return Objects.requireNonNull(client.keys(key)).stream();
    }
}
