package io.github.imaping.token.api.jwt;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import io.github.imaping.token.api.model.DefaultJwtAccessToken;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAccessTokenCodecTest {

    @Test
    void shouldReturnPlainTokenWhenJwtDisabled() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        DefaultAccessTokenCodec codec = new DefaultAccessTokenCodec(properties);

        DecodedAccessToken decoded = codec.decode("AT-plain");

        assertEquals("AT-plain", decoded.getTokenId());
        assertFalse(decoded.isJwt());
    }

    @Test
    void shouldEncodeAndDecodeJwtToken() {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().setCreateAsJwt(true);
        DefaultAccessTokenCodec codec = new DefaultAccessTokenCodec(properties);
        DefaultJwtAccessToken token = new DefaultJwtAccessToken("AT-registry", new HardTimeoutExpirationPolicy(3600), authentication("user-1"));

        String jwt = codec.encode(token);
        DecodedAccessToken decoded = codec.decode(jwt);

        assertTrue(jwt.contains("."));
        assertEquals("AT-registry", decoded.getTokenId());
        assertTrue(decoded.isJwt());
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
