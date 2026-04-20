package io.github.imaping.token.api.registry;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.expiration.TimeoutExpirationPolicy;
import io.github.imaping.token.api.exception.ConcurrentSessionControlException;
import io.github.imaping.token.api.lock.LockRepository;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.configuration.model.token.ConcurrentSessionOverflowStrategy;
import io.github.imaping.token.configuration.model.token.ConcurrentSessionProperties;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentSessionControlTokenRegistryTest {

    @Test
    void shouldInvalidateOldestSessionWhenOverflowStrategyIsInvalidateOldest() throws Exception {
        DefaultTokenRegistry delegate = new DefaultTokenRegistry();
        ConcurrentSessionProperties properties = new ConcurrentSessionProperties()
                .setEnabled(true)
                .setMaxSessions(1)
                .setEnabledTokenTypes(List.of("TimeoutAccessToken"));
        ConcurrentSessionControlTokenRegistry registry =
                new ConcurrentSessionControlTokenRegistry(delegate, LockRepository.noOp(), properties);

        DefaultTimeoutAccessToken oldest = token("AT-oldest", "user-1", Instant.parse("2026-04-20T09:00:00Z"));
        DefaultTimeoutAccessToken latest = token("AT-latest", "user-1", Instant.parse("2026-04-20T10:00:00Z"));

        registry.addToken(oldest);
        registry.addToken(latest);

        assertNull(delegate.getToken(oldest.getId()));
        assertNotNull(delegate.getToken(latest.getId()));
        assertEquals(1, delegate.countSessionsFor("user-1"));
    }

    @Test
    void shouldRejectNewLoginWhenOverflowStrategyIsDenyNewLogin() throws Exception {
        DefaultTokenRegistry delegate = new DefaultTokenRegistry();
        ConcurrentSessionProperties properties = new ConcurrentSessionProperties()
                .setEnabled(true)
                .setMaxSessions(1)
                .setOverflowStrategy(ConcurrentSessionOverflowStrategy.DENY_NEW_LOGIN)
                .setEnabledTokenTypes(List.of("TimeoutAccessToken"));
        ConcurrentSessionControlTokenRegistry registry =
                new ConcurrentSessionControlTokenRegistry(delegate, LockRepository.noOp(), properties);

        DefaultTimeoutAccessToken oldest = token("AT-oldest", "user-1", Instant.parse("2026-04-20T09:00:00Z"));
        DefaultTimeoutAccessToken latest = token("AT-latest", "user-1", Instant.parse("2026-04-20T10:00:00Z"));

        registry.addToken(oldest);

        assertThrows(ConcurrentSessionControlException.class, () -> registry.addToken(latest));
        assertNotNull(delegate.getToken(oldest.getId()));
        assertNull(delegate.getToken(latest.getId()));
    }

    private DefaultTimeoutAccessToken token(final String tokenId, final String principalId, final Instant createdAt) {
        TimeoutExpirationPolicy policy = new TimeoutExpirationPolicy(300);
        DefaultTimeoutAccessToken token = new DefaultTimeoutAccessToken(tokenId, policy, authentication(principalId));
        token.setCreationTime(createdAt.atZone(ZoneOffset.UTC));
        token.setLastTimeUsed(ZonedDateTime.now(ZoneOffset.UTC));
        return token;
    }

    private Authentication<String> authentication(final String principalId) {
        return Authentication.<String>builder()
                .principal(Principal.<String>builder()
                        .id(principalId)
                        .userInfo(BaseUserInfo.<String>builder()
                                .id(principalId)
                                .loginName(principalId)
                                .build())
                        .build())
                .build();
    }
}
