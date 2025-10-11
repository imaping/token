package com.imaping.token.resource.client.filter;

import com.imaping.token.api.authentication.DefaultBearerTokenAuthenticationToken;
import com.imaping.token.api.exception.TokenAuthenticationException;
import com.imaping.token.api.exception.TokenError;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import com.imaping.token.resource.client.authentication.TokenAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    private final AuthenticationFailureHandler authenticationFailureHandler;

    private final IMapingConfigurationProperties properties;


    public TokenAuthenticationFilter(AuthenticationManager authenticationManager, TokenAuthenticationEntryPoint authenticationEntryPoint, IMapingConfigurationProperties properties) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
        this.authenticationManager = authenticationManager;
        this.authenticationFailureHandler = authenticationEntryPoint::commence;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token;
        try {
            token = resolve(request);
        } catch (AuthenticationException invalid) {
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, invalid);
            return;
        }
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        DefaultBearerTokenAuthenticationToken authenticationRequest = new DefaultBearerTokenAuthenticationToken(token);
        try {
            Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationResult);
            SecurityContextHolder.setContext(context);
            chain.doFilter(request, response);
        } catch (AuthenticationException failed) {
            Cookie tokenCookie = new Cookie(properties.getToken().getAccessTokenName().toUpperCase(), null);
            tokenCookie.setPath("/");
            tokenCookie.setMaxAge(0);
            response.addCookie(tokenCookie);
            SecurityContextHolder.clearContext();
            if (log.isDebugEnabled()) {
                log.debug("Authentication request for failed!", failed);
            }
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
        }

    }

    private String resolve(HttpServletRequest request) {
        String cookieToken = resolveFromCookies(request.getCookies());
        String authorizationHeaderToken = resolveFromAuthorizationHeader(request);
        String parameterToken = resolveFromRequestParameters(request);
        if (cookieToken == null && authorizationHeaderToken == null && parameterToken == null) {
            return null;
        }
        TokenError error = new TokenError(TokenError.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST,
                "Found multiple bearer tokens in the request");
        if (isCookieTokenSupportedForRequest() && cookieToken != null) {
            if ((isParameterTokenSupportedForRequest(request) && parameterToken != null) || authorizationHeaderToken != null) {
                throw new TokenAuthenticationException(error);
            }
            return cookieToken;
        }
        if (isParameterTokenSupportedForRequest(request) && parameterToken != null) {
            if ((isCookieTokenSupportedForRequest() && cookieToken != null) || authorizationHeaderToken != null) {
                throw new TokenAuthenticationException(error);
            }
            return parameterToken;
        }
        if (authorizationHeaderToken != null) {
            if ((isCookieTokenSupportedForRequest() && cookieToken != null) || isParameterTokenSupportedForRequest(request) && parameterToken != null) {
                throw new TokenAuthenticationException(error);
            }
            return authorizationHeaderToken;
        }
        throw new TokenAuthenticationException(error);
    }

    private String resolveFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase(properties.getToken().getAccessTokenName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isParameterTokenSupportedForRequest(HttpServletRequest request) {
        return (("POST".equals(request.getMethod())) || ("GET".equals(request.getMethod())));
    }

    private boolean isCookieTokenSupportedForRequest() {
        return true;
    }

    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
            Pattern.CASE_INSENSITIVE);

    private String resolveFromRequestParameters(HttpServletRequest request) {
        String[] values = request.getParameterValues(properties.getToken().getAccessTokenName());
        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length == 1) {
            return values[0];
        }
        TokenError error = new TokenError(TokenError.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST,
                "Found multiple bearer tokens in the request");
        throw new TokenAuthenticationException(error);
    }

    private static String resolveFromAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
            Matcher matcher = authorizationPattern.matcher(authorization);

            if (!matcher.matches()) {
                TokenError error = new TokenError(TokenError.INVALID_TOKEN,
                        HttpStatus.UNAUTHORIZED,
                        "Bearer token is malformed");
                throw new TokenAuthenticationException(error);
            }
            return matcher.group("token");
        }
        return null;
    }
}
