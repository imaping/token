package com.imaping.token.api.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class DefaultTokenAuthentication extends AbstractAuthenticationToken {

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
