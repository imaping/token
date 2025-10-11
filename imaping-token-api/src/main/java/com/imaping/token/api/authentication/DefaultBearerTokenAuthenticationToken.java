package com.imaping.token.api.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Collections;

@Getter
public class DefaultBearerTokenAuthenticationToken extends AbstractAuthenticationToken {

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
