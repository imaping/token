package com.imaping.token.api.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

/**
 * Token 认证异常 - Token 验证失败时抛出.
 *
 * <p><b>序列化要求:</b> 异常对象可能在分布式环境中传递(RMI/RPC),
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see TokenError
 * @see org.springframework.security.core.AuthenticationException
 */
public class TokenAuthenticationException extends AuthenticationException {

    /**
     * 保留 serialVersionUID 以确保异常序列化兼容性.
     * 异常对象可能在分布式场景中跨 JVM 传递.
     */
    private static final long serialVersionUID = 1781422850787816370L;

    private TokenError error;

    public TokenAuthenticationException(TokenError error) {
        this(error, error.getDescription());
    }

    public TokenAuthenticationException(TokenError error, Throwable cause) {
        this(error, cause.getMessage(), cause);
    }

    public TokenAuthenticationException(TokenError error, String message) {
        super(message);
        this.setError(error);
    }

    public TokenAuthenticationException(TokenError error, String message, Throwable cause) {
        super(message, cause);
        this.setError(error);
    }

    public TokenError getError() {
        return this.error;
    }

    private void setError(TokenError error) {
        Assert.notNull(error, "error cannot be null");
        this.error = error;
    }
}
