package com.imaping.token.resource.client.authentication;

import com.imaping.token.api.authentication.AuthenticationAwareToken;
import com.imaping.token.api.authentication.DefaultBearerTokenAuthenticationToken;
import com.imaping.token.api.authentication.DefaultTokenAuthentication;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.api.exception.TokenAuthenticationException;
import com.imaping.token.api.exception.TokenError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationProvider implements AuthenticationProvider {
    private final TokenRegistry tokenRegistry;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof DefaultBearerTokenAuthenticationToken)) {
            return null;
        }
        DefaultBearerTokenAuthenticationToken authenticationToken = (DefaultBearerTokenAuthenticationToken) authentication;
        final String tokenId = authenticationToken.getToken();
        final Token token = tokenRegistry.getToken(tokenId);
        if (token == null || token.isExpired()) {
            log.error("Provided token [{}] is either not found in the token registry or has expired", tokenId);
            TokenError tokenError = new TokenError(TokenError.INVALID_TOKEN,
                    HttpStatus.UNAUTHORIZED,
                    String.format("Provided token [%s] is either not found in the token registry or has expired", tokenId));
            throw new TokenAuthenticationException(tokenError);
        }
        updateTokenUsage(token);
        AuthenticationAwareToken authenticationAwareToken = (AuthenticationAwareToken) token;
        Set<String> roles = authenticationAwareToken.getAuthentication().getPrincipal().getUserInfo().getRoles();
        Collection<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toCollection(() -> new ArrayList<>(roles.size())));
        return new DefaultTokenAuthentication(authenticationAwareToken.getAuthentication(), tokenId, authorities);
    }


    protected void updateTokenUsage(final Token token) {
        try {
            token.update();
            if (token.isExpired()) {
                tokenRegistry.deleteToken(token.getId());
            } else {
                tokenRegistry.updateToken(token);
            }
        } catch (Exception e) {
            log.error("update token error", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }
}
