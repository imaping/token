package io.github.imaping.token.resource.client.authentication;

import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.exception.TokenError;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenAuthenticationEntryPointTest {

    @Test
    void shouldRedirectRelativeLoginUrlWhenFeatureEnabled() throws Exception {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().getFailureRedirect().setEnabled(true);
        TokenAuthenticationEntryPoint entryPoint = new TokenAuthenticationEntryPoint(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("loginUrl", "/login");
        request.addParameter("hash", "welcome");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, invalidToken());

        assertEquals("/login#welcome", response.getRedirectedUrl());
    }

    @Test
    void shouldBlockAbsoluteRedirectByDefault() throws Exception {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().getFailureRedirect().setEnabled(true);
        TokenAuthenticationEntryPoint entryPoint = new TokenAuthenticationEntryPoint(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("loginUrl", "https://evil.example.com/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, invalidToken());

        assertNull(response.getRedirectedUrl());
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAllowWhitelistedAbsoluteRedirect() throws Exception {
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().getFailureRedirect()
                .setEnabled(true)
                .setAllowAbsoluteUrls(true)
                .getAllowedHosts().add("example.com");
        TokenAuthenticationEntryPoint entryPoint = new TokenAuthenticationEntryPoint(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("loginUrl", "https://example.com/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, invalidToken());

        assertEquals("https://example.com/login", response.getRedirectedUrl());
    }

    private TokenAuthenticationException invalidToken() {
        return new TokenAuthenticationException(new TokenError(TokenError.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, "invalid"));
    }
}
