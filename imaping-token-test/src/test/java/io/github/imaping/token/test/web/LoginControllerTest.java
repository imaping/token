package io.github.imaping.token.test.web;

import io.github.imaping.token.api.refresh.TokenGrant;
import io.github.imaping.token.api.refresh.TokenRefreshService;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    @Test
    void shouldWriteAccessAndRefreshCookieOnLogin() throws Exception {
        TokenRegistry tokenRegistry = mock(TokenRegistry.class);
        TokenRefreshService tokenRefreshService = mock(TokenRefreshService.class);
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        LoginController controller = new LoginController(tokenRegistry, tokenRefreshService, properties);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenRefreshService.issue(org.mockito.ArgumentMatchers.any())).thenReturn(TokenGrant.builder()
                .tokenType("Bearer")
                .accessToken("AT-1")
                .refreshToken("RT-1")
                .refreshTokenExpiresAt(ZonedDateTime.now().plusDays(30))
                .build());

        controller.login("alice", "pwd", response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(2, cookies.size());
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith(properties.getAccessTokenName() + "=AT-1")));
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith(properties.getRefreshToken().getCookieName() + "=RT-1")));
        assertTrue(cookies.stream().anyMatch(value -> value.contains("HttpOnly")));
    }

    @Test
    void shouldRefreshUsingCookieWhenParameterMissing() throws Exception {
        TokenRegistry tokenRegistry = mock(TokenRegistry.class);
        TokenRefreshService tokenRefreshService = mock(TokenRefreshService.class);
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        LoginController controller = new LoginController(tokenRegistry, tokenRefreshService, properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(properties.getRefreshToken().getCookieName(), "RT-cookie"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenRefreshService.refresh("RT-cookie")).thenReturn(TokenGrant.builder()
                .tokenType("Bearer")
                .accessToken("AT-2")
                .refreshToken("RT-2")
                .refreshTokenExpiresAt(ZonedDateTime.now().plusDays(30))
                .build());

        controller.refresh(null, request, response);

        verify(tokenRefreshService).refresh("RT-cookie");
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith(properties.getRefreshToken().getCookieName() + "=RT-2")));
    }

    @Test
    void shouldLogoutUsingRefreshCookieAndClearBothCookies() throws Exception {
        TokenRegistry tokenRegistry = mock(TokenRegistry.class);
        TokenRefreshService tokenRefreshService = mock(TokenRefreshService.class);
        IMapingTokenConfigurationProperties properties = new IMapingTokenConfigurationProperties();
        LoginController controller = new LoginController(tokenRegistry, tokenRefreshService, properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(properties.getRefreshToken().getCookieName(), "RT-cookie"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenRefreshService.revokeGrant("RT-cookie")).thenReturn(2L);

        controller.logout(null, request, response);

        verify(tokenRefreshService).revokeGrant("RT-cookie");
        verifyNoInteractions(tokenRegistry);
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertEquals(2, cookies.size());
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith(properties.getAccessTokenName() + "=") && value.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(value -> value.startsWith(properties.getRefreshToken().getCookieName() + "=") && value.contains("Max-Age=0")));
    }
}
