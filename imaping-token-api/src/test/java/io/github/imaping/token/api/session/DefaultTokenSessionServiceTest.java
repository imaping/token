package io.github.imaping.token.api.session;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.expiration.TimeoutExpirationPolicy;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.api.registry.DefaultTokenRegistry;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTokenSessionServiceTest {

    @Test
    void shouldListSessionsAndMarkCurrentSession() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        DefaultTokenSessionService sessionService = new DefaultTokenSessionService(tokenRegistry);
        DefaultTimeoutAccessToken first = token("AT-1", "user-1");
        DefaultTimeoutAccessToken second = token("AT-2", "user-1");
        tokenRegistry.addToken(first);
        tokenRegistry.addToken(second);

        List<TokenSession> sessions = sessionService.getSessionsFor("user-1", "AT-2");

        assertEquals(2, sessions.size());
        assertTrue(sessions.stream().anyMatch(session -> "AT-2".equals(session.getTokenId()) && session.isCurrent()));
        assertTrue(sessions.stream().anyMatch(session -> "AT-1".equals(session.getTokenId()) && !session.isCurrent()));
    }

    @Test
    void shouldRevokeOnlyOwnedSession() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        DefaultTokenSessionService sessionService = new DefaultTokenSessionService(tokenRegistry);
        DefaultTimeoutAccessToken owned = token("AT-1", "user-1");
        DefaultTimeoutAccessToken foreign = token("AT-2", "user-2");
        tokenRegistry.addToken(owned);
        tokenRegistry.addToken(foreign);

        int deleted = sessionService.revokeSession("user-1", owned.getId());

        assertEquals(1, deleted);
        assertNull(tokenRegistry.getToken(owned.getId()));
        assertNotNull(tokenRegistry.getToken(foreign.getId()));
        assertEquals(0, sessionService.revokeSession("user-1", foreign.getId()));
    }

    @Test
    void shouldRevokeOtherSessionsOnly() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        DefaultTokenSessionService sessionService = new DefaultTokenSessionService(tokenRegistry);
        DefaultTimeoutAccessToken current = token("AT-1", "user-1");
        DefaultTimeoutAccessToken other = token("AT-2", "user-1");
        DefaultTimeoutAccessToken foreign = token("AT-3", "user-2");
        tokenRegistry.addToken(current);
        tokenRegistry.addToken(other);
        tokenRegistry.addToken(foreign);

        long deleted = sessionService.revokeOtherSessions("user-1", current.getId());

        assertEquals(1, deleted);
        assertNotNull(tokenRegistry.getToken(current.getId()));
        assertNull(tokenRegistry.getToken(other.getId()));
        assertNotNull(tokenRegistry.getToken(foreign.getId()));
        assertTrue(sessionService.isSessionActive("user-1", current.getId()));
        assertFalse(sessionService.isSessionActive("user-1", other.getId()));
    }

    private DefaultTimeoutAccessToken token(final String tokenId, final String principalId) {
        return new DefaultTimeoutAccessToken(tokenId, new TimeoutExpirationPolicy(600), authentication(principalId));
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
