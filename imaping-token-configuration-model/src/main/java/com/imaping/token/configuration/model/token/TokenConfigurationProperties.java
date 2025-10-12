package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Token 配置属性类.
 *
 * <p>绑定 {@code imaping.token.*} 配置项.</p>
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenConfigurationProperties {

    @NestedConfigurationProperty
    private TokenRegistryProperties registry = new TokenRegistryProperties();

    @NestedConfigurationProperty
    private AccessTokenProperties accessToken = new AccessTokenProperties();


    /**
     * token使用名称，防止部署在同一个域名下系统token冲突
     */
    private String accessTokenName = "access_token";
}
