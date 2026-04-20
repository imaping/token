package io.github.imaping.token.resource.client.config;

import io.github.imaping.token.api.factory.TokenFactory;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TokenSecurityConfigDefaultsTest {

    @Test
    void shouldRequireExplicitGetExposure() {
        ExposedTokenSecurityConfig config = new ExposedTokenSecurityConfig();

        Map<HttpMethod, String[]> permitMatchers = config.permitMatchersWithMethod();

        assertFalse(permitMatchers.containsKey(HttpMethod.GET));
        assertTrue(permitMatchers.containsKey(HttpMethod.OPTIONS));
        assertArrayEquals(new String[]{"/error"}, config.permitMatchers());
    }

    private static final class ExposedTokenSecurityConfig extends TokenSecurityConfig {
        private ExposedTokenSecurityConfig() {
            super(mock(TokenRegistry.class), mock(TokenFactory.class), mock(AccessTokenCodec.class), new IMapingTokenConfigurationProperties());
        }

        private Map<HttpMethod, String[]> permitMatchersWithMethod() {
            return super.getPermitAntMatchersWithMethod();
        }

        private String[] permitMatchers() {
            return super.getPermitAntMatchers();
        }
    }
}
