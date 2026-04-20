package io.github.imaping.token.api.registry;

import com.github.benmanes.caffeine.cache.*;
import io.github.imaping.token.api.model.Token;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.time.ZonedDateTime;
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

        private static long getExpiration(final Token value) {
            if (value.isExpired()) {
                log.debug("Token [{}] has expired and shall be evicted from the cache", value.getId());
                return 0;
            }
            ZonedDateTime now = ZonedDateTime.now(value.getExpirationPolicy().getClock());
            long remainingToLive = remainingNanos(now, value.getCreationTime(), value.getExpirationPolicy().getTimeToLive());
            long remainingToIdle = remainingNanos(now, value.getLastTimeUsed(), value.getExpirationPolicy().getTimeToIdle());
            if (remainingToLive > 0 && remainingToIdle > 0) {
                return Math.min(remainingToLive, remainingToIdle);
            }
            return Math.max(remainingToLive, remainingToIdle);
        }

        private static long remainingNanos(final ZonedDateTime now, final ZonedDateTime baseTime, final Long seconds) {
            if (seconds == null || seconds <= 0 || baseTime == null) {
                return 0;
            }
            Duration duration = Duration.between(now, baseTime.plusSeconds(seconds));
            return Math.max(duration.toNanos(), 0);
        }

        @Override
        public long expireAfterCreate(final String key, final Token value, final long currentTime) {
            return getExpiration(value);
        }

        @Override
        public long expireAfterUpdate(final String key, final Token value, final long currentTime, final long currentDuration) {
            return getExpiration(value);
        }

        @Override
        public long expireAfterRead(final String key, final Token value, final long currentTime, final long currentDuration) {
            return getExpiration(value);
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

