package io.github.imaping.token.resource.client.authentication;

import io.github.imaping.token.api.authentication.AuthenticationAwareToken;
import io.github.imaping.token.api.authentication.DefaultBearerTokenAuthenticationToken;
import io.github.imaping.token.api.authentication.DefaultTokenAuthentication;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.jwt.DecodedAccessToken;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.exception.TokenError;
import io.jsonwebtoken.JwtException;
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
    private final AccessTokenCodec accessTokenCodec;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof DefaultBearerTokenAuthenticationToken)) {
            return null;
        }
        DefaultBearerTokenAuthenticationToken authenticationToken = (DefaultBearerTokenAuthenticationToken) authentication;
        final DecodedAccessToken decodedToken;
        try {
            decodedToken = accessTokenCodec.decode(authenticationToken.getToken());
        } catch (JwtException ex) {
            TokenError tokenError = new TokenError(TokenError.INVALID_TOKEN,
                    HttpStatus.UNAUTHORIZED,
                    "Provided access token is malformed or signature validation failed");
            throw new TokenAuthenticationException(tokenError, ex);
        }
        final String tokenId = decodedToken.getTokenId();
        final Token token = tokenRegistry.getToken(tokenId);
        if (token == null || token.isExpired()) {
            log.error("Provided token [{}] is either not found in the token registry or has expired", tokenId);
            TokenError tokenError = new TokenError(TokenError.INVALID_TOKEN,
                    HttpStatus.UNAUTHORIZED,
                    String.format("Provided token [%s] is either not found in the token registry or has expired", tokenId));
            throw new TokenAuthenticationException(tokenError);
        }
        if (token instanceof RefreshToken) {
            TokenError tokenError = new TokenError(TokenError.INVALID_TOKEN,
                    HttpStatus.UNAUTHORIZED,
                    String.format("Provided token [%s] is a refresh token and cannot be used for resource access", tokenId));
            throw new TokenAuthenticationException(tokenError);
        }
        updateTokenUsage(token);
        AuthenticationAwareToken authenticationAwareToken = (AuthenticationAwareToken) token;
        Set<String> roles = authenticationAwareToken.getAuthentication().getPrincipal().getUserInfo().getRoles();
        Collection<SimpleGrantedAuthority> authorities = null;
        if (roles != null) authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toCollection(() -> new ArrayList<>(roles.size())));
        return new DefaultTokenAuthentication(authenticationAwareToken.getAuthentication(), decodedToken.getTokenValue(), tokenId, authorities);
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
        return DefaultBearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

