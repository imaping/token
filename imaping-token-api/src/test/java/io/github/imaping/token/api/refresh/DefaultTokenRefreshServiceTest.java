package io.github.imaping.token.api.refresh;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.factory.RefreshTokenDefaultFactory;
import io.github.imaping.token.api.factory.RefreshTokenFactory;
import io.github.imaping.token.api.factory.TimeoutTokenFactory;
import io.github.imaping.token.api.generator.DefaultUniqueTokenIdGenerator;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.jwt.DefaultAccessTokenCodec;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.registry.DefaultTokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTokenRefreshServiceTest {

    @Test
    void shouldIssueAccessAndRefreshTokenGrant() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        DefaultTokenRefreshService service = service(tokenRegistry, properties);

        TokenGrant grant = service.issue(authentication("user-1"));

        assertNotNull(grant.getAccessToken());
        assertNotNull(grant.getRefreshToken());
        assertEquals("Bearer", grant.getTokenType());
        assertNotNull(tokenRegistry.getToken(grant.getAccessToken(), TimeoutAccessToken.class));
        RefreshToken refreshToken = tokenRegistry.getToken(grant.getRefreshToken(), RefreshToken.class);
        assertEquals(grant.getAccessToken(), refreshToken.getAccessTokenId());
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeOldTokensOnRefresh() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        DefaultTokenRefreshService service = service(tokenRegistry, properties);
        TokenGrant firstGrant = service.issue(authentication("user-1"));

        TokenGrant secondGrant = service.refresh(firstGrant.getRefreshToken());

        assertNotEquals(firstGrant.getAccessToken(), secondGrant.getAccessToken());
        assertNotEquals(firstGrant.getRefreshToken(), secondGrant.getRefreshToken());
        assertNull(tokenRegistry.getToken(firstGrant.getAccessToken()));
        assertNull(tokenRegistry.getToken(firstGrant.getRefreshToken()));
        assertNotNull(tokenRegistry.getToken(secondGrant.getAccessToken(), TimeoutAccessToken.class));
        assertNotNull(tokenRegistry.getToken(secondGrant.getRefreshToken(), RefreshToken.class));
    }

    @Test
    void shouldSupportDisabledRefreshTokenMode() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getRefreshToken().setEnabled(false);
        DefaultTokenRefreshService service = service(tokenRegistry, properties);

        TokenGrant grant = service.issue(authentication("user-1"));

        assertNotNull(grant.getAccessToken());
        assertNull(grant.getRefreshToken());
        assertFalse(service.isEnabled());
    }

    @Test
    void shouldIssueJwtAccessTokenWhenJwtModeEnabled() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);
        DefaultTokenRefreshService service = service(tokenRegistry, properties);

        TokenGrant grant = service.issue(authentication("user-1"));

        assertTrue(grant.getAccessToken().contains("."));
        assertNotNull(grant.getRefreshToken());
    }

    @Test
    void shouldRevokeGrantByRefreshToken() throws Exception {
        DefaultTokenRegistry tokenRegistry = new DefaultTokenRegistry();
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        DefaultTokenRefreshService service = service(tokenRegistry, properties);
        TokenGrant grant = service.issue(authentication("user-1"));

        long deleted = service.revokeGrant(grant.getRefreshToken());

        assertTrue(deleted >= 2);
        assertNull(tokenRegistry.getToken(grant.getAccessToken()));
        assertNull(tokenRegistry.getToken(grant.getRefreshToken()));
    }

    private DefaultTokenRefreshService service(final DefaultTokenRegistry tokenRegistry,
                                               final IMapingTokenConfigurationProperties properties) {
        DefaultUniqueTokenIdGenerator idGenerator = new DefaultUniqueTokenIdGenerator();
        RefreshTokenFactory refreshTokenFactory = new RefreshTokenDefaultFactory(idGenerator, properties);
        AccessTokenCodec accessTokenCodec = new DefaultAccessTokenCodec(properties);
        TimeoutTokenFactory accessFactory = new TimeoutTokenFactory() {
            @Override
            public io.github.imaping.token.api.model.TimeoutAccessToken create(final Authentication<?> authentication) {
                return new io.github.imaping.token.api.model.DefaultTimeoutAccessToken(
                        idGenerator.getNewTokenId(io.github.imaping.token.api.model.TimeoutAccessToken.PREFIX),
                        new io.github.imaping.token.api.expiration.TimeoutExpirationPolicy(3600),
                        authentication
                );
            }

            @Override
            public Class<? extends io.github.imaping.token.api.model.Token> getTokenType() {
                return io.github.imaping.token.api.model.TimeoutAccessToken.class;
            }
        };
        return new DefaultTokenRefreshService(tokenRegistry, accessFactory, refreshTokenFactory, properties, accessTokenCodec);
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
