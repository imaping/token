package com.imaping.token.api.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

/**
 * token 认证异常
 */
public class TokenAuthenticationException extends AuthenticationException {

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
