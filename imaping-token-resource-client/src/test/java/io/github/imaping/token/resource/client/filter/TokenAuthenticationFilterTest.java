package io.github.imaping.token.resource.client.filter;

import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.resource.client.authentication.TokenAuthenticationEntryPoint;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreRequestParameterByDefault() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(authenticationManager, new TokenAuthenticationEntryPoint(properties), properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addParameter(properties.getAccessTokenName(), "token-from-param");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(authenticationManager, never()).authenticate(any());
        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAuthenticateFromBearerHeader() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any())).thenReturn(new TestingAuthenticationToken("principal", "token"));
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(authenticationManager, new TokenAuthenticationEntryPoint(properties), properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldClearCookieWithSecurityAttributesWhenAuthenticationFails() throws Exception {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        properties.getAccessToken().getCookie().setSecure(true).setSameSite("Strict");
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(authenticationManager, new TokenAuthenticationEntryPoint(properties), properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setCookies(new Cookie(properties.getAccessTokenName(), "token-from-cookie"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String setCookie = response.getHeader("Set-Cookie");
        assertEquals(401, response.getStatus());
        assertTrue(setCookie.contains(properties.getAccessTokenName() + "="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Strict"));
    }
}
