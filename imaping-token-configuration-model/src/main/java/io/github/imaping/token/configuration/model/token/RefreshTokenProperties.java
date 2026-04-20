package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Refresh Token 配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class RefreshTokenProperties {

    /**
     * 是否启用 refresh token 机制。
     */
    private boolean enabled = true;

    /**
     * Refresh token 固定有效期。
     */
    private String timeToKillInSeconds = "P30D";

    /**
     * Refresh token Cookie 名称。
     */
    private String cookieName = "refresh_token";

    /**
     * Refresh token Cookie 安全配置。
     */
    @NestedConfigurationProperty
    private TokenCookieProperties cookie = new TokenCookieProperties().setSameSite("Strict");
}
