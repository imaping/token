package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 访问令牌配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class AccessTokenProperties {

    private String timeToKillInSeconds = "PT2H";

    private boolean createAsJwt;

    /**
     * Token 传输介质配置。
     */
    @NestedConfigurationProperty
    private TokenTransportProperties transport = new TokenTransportProperties();

    /**
     * Cookie 安全属性配置。
     */
    @NestedConfigurationProperty
    private TokenCookieProperties cookie = new TokenCookieProperties();

    /**
     * 认证失败后的跳转安全配置。
     */
    @NestedConfigurationProperty
    private TokenFailureRedirectProperties failureRedirect = new TokenFailureRedirectProperties();

    /**
     * JWT 配置。
     */
    @NestedConfigurationProperty
    private TokenJwtProperties jwt = new TokenJwtProperties();
}

