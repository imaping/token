package io.github.imaping.token.api.config;

import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenApiConfigJwtValidationTest {

    private final TokenApiConfig config = new TokenApiConfig();

    @Test
    void shouldRejectDefaultJwtSecretWhenJwtEnabled() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);

        assertThrows(IllegalStateException.class, () -> config.jwtConfigurationValidator(properties));
    }

    @Test
    void shouldRejectWeakJwtSecretWhenJwtEnabled() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);
        properties.getAccessToken().getJwt()
                .setSecret("short-secret")
                .setIssuer("issuer")
                .setAudience("audience");

        assertThrows(IllegalStateException.class, () -> config.jwtConfigurationValidator(properties));
    }

    @Test
    void shouldAllowStrongJwtSecretWhenJwtEnabled() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);
        properties.getAccessToken().getJwt()
                .setSecret("TokenApiConfigJwtValidationStrongSecret1234567890")
                .setIssuer("issuer")
                .setAudience("audience");

        assertDoesNotThrow(() -> config.jwtConfigurationValidator(properties));
    }
}
