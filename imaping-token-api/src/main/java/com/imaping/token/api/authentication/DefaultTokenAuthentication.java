package com.imaping.token.api.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * 默认的 Token 认证实现 - 封装 Token 认证信息.
 *
 * <p><b>序列化要求:</b> 作为 Spring Security 认证对象,可能在分布式环境中传递,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see Authentication
 * @see org.springframework.security.authentication.AbstractAuthenticationToken
 */
@Getter
public class DefaultTokenAuthentication extends AbstractAuthenticationToken {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * 作为 Spring Security 认证对象,可能跨 JVM 传递.
     */
    @Serial
    private static final long serialVersionUID = -8103211718320156900L;
    private final Authentication authentication;

    private final String token;

    public DefaultTokenAuthentication(Authentication authentication, String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.authentication = authentication;
        this.token = token;
        setAuthenticated(true);
    }

    public DefaultTokenAuthentication(Authentication authentication, String token) {
        this(authentication, token, null);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return authentication;
    }
}
