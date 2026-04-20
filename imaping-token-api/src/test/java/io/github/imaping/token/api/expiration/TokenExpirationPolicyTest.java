package io.github.imaping.token.api.expiration;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.model.DefaultHardTimeoutToken;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenExpirationPolicyTest {

    @Test
    void shouldExpireTimeoutTokenByIdleWindow() {
        TimeoutExpirationPolicy policy = new TimeoutExpirationPolicy(5);
        DefaultTimeoutAccessToken token = new DefaultTimeoutAccessToken("AT-1", policy, authentication("user-1"));
        token.setLastTimeUsed(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(3));

        assertFalse(token.isExpired());

        token.setLastTimeUsed(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(6));
        assertTrue(token.isExpired());
    }

    @Test
    void shouldExpireHardTimeoutTokenByCreationTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-20T10:00:00Z"), ZoneOffset.UTC);
        HardTimeoutExpirationPolicy policy = new HardTimeoutExpirationPolicy(5);
        policy.setClock(clock);
        DefaultHardTimeoutToken token = new DefaultHardTimeoutToken("ATT-1", policy, authentication("user-1"), "code", "desc");
        token.setCreationTime(token.getCreationTime().minusSeconds(6));

        assertTrue(token.isExpired());

        token.setCreationTime(token.getCreationTime().plusSeconds(2));
        assertFalse(token.isExpired());
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
