package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class TokenConfigurationProperties implements Serializable {

    private static final long serialVersionUID = -5584374037877665297L;
    @NestedConfigurationProperty
    private TokenRegistryProperties registry = new TokenRegistryProperties();

    @NestedConfigurationProperty
    private AccessTokenProperties accessToken = new AccessTokenProperties();


    /**
     * token使用名称，防止部署在同一个域名下系统token冲突
     */
    private String accessTokenName = "access_token";
}
