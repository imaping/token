package com.imaping.token.api.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Collections;

/**
 * Bearer Token 认证令牌 - 封装 Bearer Token 认证请求.
 *
 * <p><b>序列化要求:</b> 作为 Spring Security 认证令牌,可能在分布式环境中传递,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see org.springframework.security.authentication.AbstractAuthenticationToken
 */
@Getter
public class DefaultBearerTokenAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * 作为 Spring Security 认证令牌,可能跨 JVM 传递.
     */
    @Serial
    private static final long serialVersionUID = 8956942357463929399L;
    private final String token;

    public DefaultBearerTokenAuthenticationToken(String token) {
        super(Collections.emptyList());
        Assert.hasText(token, "token cannot be empty");
        this.token = token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }
}
