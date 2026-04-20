package io.github.imaping.token.resource.client.authentication;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.DefaultBearerTokenAuthenticationToken;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import io.github.imaping.token.api.expiration.TimeoutExpirationPolicy;
import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.jwt.DefaultAccessTokenCodec;
import io.github.imaping.token.api.model.DefaultJwtAccessToken;
import io.github.imaping.token.api.model.DefaultRefreshToken;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenAuthenticationProviderTest {

    @Test
    void shouldRejectRefreshTokenForResourceAuthentication() {
        TokenRegistry tokenRegistry = mock(TokenRegistry.class);
        AccessTokenCodec accessTokenCodec = mock(AccessTokenCodec.class);
        RefreshToken refreshToken = new DefaultRefreshToken("RT-1", new HardTimeoutExpirationPolicy(3600), authentication("user-1"), "AT-1");
        when(accessTokenCodec.decode("RT-1")).thenReturn(io.github.imaping.token.api.jwt.DecodedAccessToken.plain("RT-1"));
        when(tokenRegistry.getToken("RT-1")).thenReturn(refreshToken);
        TokenAuthenticationProvider provider = new TokenAuthenticationProvider(tokenRegistry, accessTokenCodec);

        assertThrows(TokenAuthenticationException.class,
                () -> provider.authenticate(new DefaultBearerTokenAuthenticationToken("RT-1")));
    }

    @Test
    void shouldAuthenticateJwtAccessTokenUsingRegistryTokenId() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);
        AccessTokenCodec accessTokenCodec = new DefaultAccessTokenCodec(properties);
        TokenRegistry tokenRegistry = mock(TokenRegistry.class);
        DefaultJwtAccessToken accessToken = new DefaultJwtAccessToken("AT-registry", new HardTimeoutExpirationPolicy(3600), authentication("user-1"));
        String jwt = accessTokenCodec.encode(accessToken);
        when(tokenRegistry.getToken("AT-registry")).thenReturn(accessToken);
        TokenAuthenticationProvider provider = new TokenAuthenticationProvider(tokenRegistry, accessTokenCodec);

        var authentication = provider.authenticate(new DefaultBearerTokenAuthenticationToken(jwt));

        assertNotNull(authentication);
        assertEquals(jwt, ((io.github.imaping.token.api.authentication.DefaultTokenAuthentication) authentication).getToken());
        assertEquals("AT-registry", ((io.github.imaping.token.api.authentication.DefaultTokenAuthentication) authentication).getTokenId());
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
