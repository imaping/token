package com.imaping.token.api.registry;

import com.github.benmanes.caffeine.cache.*;
import com.imaping.token.api.model.Token;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.Map;

@Slf4j
@Getter
public class CachingTokenRegistry extends AbstractMapBasedTokenRegistry {

    private static final int INITIAL_CACHE_SIZE = 50;

    private static final long MAX_CACHE_SIZE = 100_000_000;

    private final Map<String, Token> mapInstance;

    private final Cache<String, Token> storage;


    public CachingTokenRegistry() {
        this.storage = Caffeine.newBuilder()
                .initialCapacity(INITIAL_CACHE_SIZE)
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfter(new CachedTokenExpirationPolicy()).removalListener(new CachedTokenRemovalListener())
                .build();
        this.mapInstance = this.storage.asMap();
    }

    /**
     * The cached token expiration policy.
     */
    public static class CachedTokenExpirationPolicy implements Expiry<String, Token> {

        private static long getExpiration(final Token value, final long currentTime) {
            if (value.isExpired()) {
                log.debug("Token [{}] has expired and shall be evicted from the cache", value.getId());
                return 0;
            }
            return currentTime;
        }

        @Override
        public long expireAfterCreate(final String key, final Token value, final long currentTime) {
            return getExpiration(value, currentTime);
        }

        @Override
        public long expireAfterUpdate(final String key, final Token value, final long currentTime, final long currentDuration) {
            return getExpiration(value, currentDuration);
        }

        @Override
        public long expireAfterRead(final String key, final Token value, final long currentTime, final long currentDuration) {
            return getExpiration(value, currentDuration);
        }
    }

    /**
     * The cached token removal listener.
     */
    public class CachedTokenRemovalListener implements RemovalListener<String, Token> {

        @Override
        public void onRemoval(final String key, final Token value, final RemovalCause cause) {
            if (cause == RemovalCause.EXPIRED) {
                log.warn("Received removal notification for token [{}] with cause [{}]. Cleaning...", key, cause);
            }
        }
    }
}
