package io.github.imaping.token.resource.client.filter;

import io.github.imaping.token.api.authentication.DefaultBearerTokenAuthenticationToken;
import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.exception.TokenError;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.configuration.model.token.TokenCookieProperties;
import io.github.imaping.token.configuration.model.token.TokenTransportProperties;
import io.github.imaping.token.resource.client.authentication.TokenAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

    private final IMapingTokenConfigurationProperties properties;
    private final TokenTransportProperties transportProperties;
    private final TokenCookieProperties cookieProperties;


    public TokenAuthenticationFilter(AuthenticationManager authenticationManager, TokenAuthenticationEntryPoint authenticationEntryPoint, IMapingTokenConfigurationProperties properties) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
        this.authenticationManager = authenticationManager;
        this.authenticationFailureHandler = authenticationEntryPoint::commence;
        this.properties = properties;
        this.transportProperties = properties.getAccessToken().getTransport();
        this.cookieProperties = properties.getAccessToken().getCookie();
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
            clearTokenCookie(response);
            SecurityContextHolder.clearContext();
            if (log.isDebugEnabled()) {
                log.debug("Authentication request for failed!", failed);
            }
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
        }

    }

    private String resolve(HttpServletRequest request) {
        String cookieToken = resolveFromCookies(request.getCookies());
        String authorizationHeaderToken = resolveFromHeaders(request);
        String parameterToken = resolveFromRequestParameters(request);
        if (cookieToken == null && authorizationHeaderToken == null && parameterToken == null) {
            return null;
        }
        if (isCookieTokenSupportedForRequest() && cookieToken != null) {
            if ((isParameterTokenSupportedForRequest(request) && parameterToken != null) || authorizationHeaderToken != null) {
                throw multipleBearerTokenException();
            }
            return cookieToken;
        }
        if (isParameterTokenSupportedForRequest(request) && parameterToken != null) {
            if ((isCookieTokenSupportedForRequest() && cookieToken != null) || authorizationHeaderToken != null) {
                throw multipleBearerTokenException();
            }
            return parameterToken;
        }
        if (authorizationHeaderToken != null) {
            if ((isCookieTokenSupportedForRequest() && cookieToken != null) || isParameterTokenSupportedForRequest(request) && parameterToken != null) {
                throw multipleBearerTokenException();
            }
            return authorizationHeaderToken;
        }
        throw multipleBearerTokenException();
    }

    private String resolveFromCookies(Cookie[] cookies) {
        if (!isCookieTokenSupportedForRequest()) {
            return null;
        }
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase(properties.getAccessTokenName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isParameterTokenSupportedForRequest(HttpServletRequest request) {
        return transportProperties.isAllowRequestParameter()
                && (("POST".equals(request.getMethod())) || ("GET".equals(request.getMethod())));
    }

    private boolean isCookieTokenSupportedForRequest() {
        return transportProperties.isAllowCookie();
    }

    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
            Pattern.CASE_INSENSITIVE);

    private String resolveFromRequestParameters(HttpServletRequest request) {
        if (!transportProperties.isAllowRequestParameter()) {
            return null;
        }
        String[] values = request.getParameterValues(properties.getAccessTokenName());
        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length == 1) {
            return values[0];
        }
        throw multipleBearerTokenException();
    }

    private String resolveFromHeaders(final HttpServletRequest request) {
        String authorizationHeaderToken = resolveFromAuthorizationHeader(request);
        String namedHeaderToken = resolveFromNamedHeader(request);
        if (!StringUtils.hasText(authorizationHeaderToken)) {
            return namedHeaderToken;
        }
        if (!StringUtils.hasText(namedHeaderToken) || authorizationHeaderToken.equals(namedHeaderToken)) {
            return authorizationHeaderToken;
        }
        throw multipleBearerTokenException();
    }

    private String resolveFromNamedHeader(final HttpServletRequest request) {
        if (!transportProperties.isAllowNamedHeader()) {
            return null;
        }
        final String token = request.getHeader(properties.getAccessTokenName());
        return StringUtils.hasText(token) ? token.trim() : null;
    }

    private String resolveFromAuthorizationHeader(HttpServletRequest request) {
        if (!transportProperties.isAllowAuthorizationHeader()) {
            return null;
        }
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

    private TokenAuthenticationException multipleBearerTokenException() {
        TokenError error = new TokenError(TokenError.INVALID_REQUEST,
                HttpStatus.BAD_REQUEST,
                "Found multiple bearer tokens in the request");
        return new TokenAuthenticationException(error);
    }

    private void clearTokenCookie(final HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getAccessTokenName(), "")
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .maxAge(0);
        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }
        if (StringUtils.hasText(cookieProperties.getSameSite())) {
            builder.sameSite(cookieProperties.getSameSite());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}

