package io.github.imaping.token.resource.client.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.exception.TokenError;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final IMapingTokenConfigurationProperties properties;

    public TokenAuthenticationEntryPoint(final IMapingTokenConfigurationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        final String redirectUrl = resolveRedirectUrl(request);
        if (StringUtils.hasLength(redirectUrl)) {
            response.sendRedirect(redirectUrl);
            return;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (exception instanceof TokenAuthenticationException tokenAuthenticationException) {
            final TokenError error = tokenAuthenticationException.getError();
            response.setStatus(error.getHttpStatus().value());
            Map<String, Object> map = new HashMap<>();
            map.put("status", 0);
            map.put("message", error.getDescription());
            map.put("content", error.getErrorCode());

            response.getOutputStream().println(objectMapper.writeValueAsString(map));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> map = new HashMap<>();
            map.put("status", 0);
            map.put("message", exception.getMessage());
            response.getOutputStream().println(objectMapper.writeValueAsString(map));
        }
    }

    private String resolveRedirectUrl(final HttpServletRequest request) {
        final var redirectProperties = properties.getAccessToken().getFailureRedirect();
        if (!redirectProperties.isEnabled()) {
            return null;
        }
        final String loginUrl = request.getParameter("loginUrl");
        if (!StringUtils.hasLength(loginUrl)) {
            return null;
        }
        if (isSafeRelativeUrl(loginUrl)) {
            return appendHash(loginUrl, request.getParameter("hash"));
        }
        if (!redirectProperties.isAllowAbsoluteUrls()) {
            return null;
        }
        try {
            final URI uri = URI.create(loginUrl);
            if (!isAllowedHost(uri.getHost(), redirectProperties.getAllowedHosts())) {
                return null;
            }
            return appendHash(loginUrl, request.getParameter("hash"));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isSafeRelativeUrl(final String loginUrl) {
        return loginUrl.startsWith("/") && !loginUrl.startsWith("//");
    }

    private boolean isAllowedHost(final String host, final List<String> allowedHosts) {
        return StringUtils.hasText(host) && allowedHosts.stream().anyMatch(host::equalsIgnoreCase);
    }

    private String appendHash(final String loginUrl, final String loginHash) {
        if (!StringUtils.hasLength(loginHash)) {
            return loginUrl;
        }
        return loginUrl + "#" + loginHash;
    }
}

