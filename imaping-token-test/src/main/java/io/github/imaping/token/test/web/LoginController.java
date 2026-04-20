package io.github.imaping.token.test.web;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.refresh.TokenGrant;
import io.github.imaping.token.api.refresh.TokenRefreshService;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.core.model.BaseUserInfo;
import io.github.imaping.token.core.util.SecurityContextUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.WebUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * 不使用单点登录，自定义登录
 */
@RestController
public class LoginController {

    private final TokenRegistry tokenRegistry;
    private final TokenRefreshService tokenRefreshService;
    private final IMapingTokenConfigurationProperties properties;

    public LoginController(final TokenRegistry tokenRegistry,
                           final TokenRefreshService tokenRefreshService,
                           IMapingTokenConfigurationProperties properties) {
        this.tokenRegistry = tokenRegistry;
        this.tokenRefreshService = tokenRefreshService;
        this.properties = properties;
    }


    @GetMapping("/rest/user-info")
    public Object userInfo() throws Exception {
        return SecurityContextUtil.getCurrentUserInfo();
    }


    @PostMapping("/login")
    public Object login(String username, String password, HttpServletResponse response) throws Exception {
        //todo: 自定义验证用户名密码，验证成功后根据用户信息生产token
        final Authentication<String> authentication = new Authentication<>(
                Principal.<String>builder()
                        .id(username)
                        .userInfo(
                                BaseUserInfo.<String>builder()
                                        .id(username)
                                        .name("test")
                                        .loginName(username)
                                        .build())
                        .build());
        TokenGrant grant = tokenRefreshService.issue(authentication);
        writeAccessTokenCookie(response, grant.getAccessToken(), -1);
        writeRefreshTokenCookie(response, grant.getRefreshToken(), grant.getRefreshTokenExpiresAt());
        return grant;
    }

    @PostMapping("/refresh")
    public Object refresh(String refreshToken, HttpServletRequest request, HttpServletResponse response) throws Exception {
        TokenGrant grant = tokenRefreshService.refresh(resolveRefreshToken(refreshToken, request));
        writeAccessTokenCookie(response, grant.getAccessToken(), -1);
        writeRefreshTokenCookie(response, grant.getRefreshToken(), grant.getRefreshTokenExpiresAt());
        return grant;
    }

    @PostMapping("/logout")
    public Object logout(String refreshToken, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //前端删除token、后端删除token 即可
        long deleted = 0;
        final String resolvedRefreshToken = resolveRefreshToken(refreshToken, request);
        if (StringUtils.hasText(resolvedRefreshToken)) {
            deleted = tokenRefreshService.revokeGrant(resolvedRefreshToken);
        } else if (StringUtils.hasText(SecurityContextUtil.getCurrentTokenId())) {
            deleted = tokenRegistry.deleteToken(SecurityContextUtil.getCurrentTokenId());
        }
        writeAccessTokenCookie(response, "", 0);
        clearRefreshTokenCookie(response);
        return deleted > 0;
    }

    private String resolveRefreshToken(final String refreshToken, final HttpServletRequest request) {
        if (StringUtils.hasText(refreshToken)) {
            return refreshToken;
        }
        Cookie cookie = WebUtils.getCookie(request, properties.getRefreshToken().getCookieName());
        return cookie != null ? cookie.getValue() : null;
    }

    private void writeAccessTokenCookie(final HttpServletResponse response, final String tokenValue, final long maxAgeSeconds) {
        final var cookieProperties = properties.getAccessToken().getCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(properties.getAccessTokenName(), tokenValue, maxAgeSeconds, cookieProperties).toString());
    }

    private void writeRefreshTokenCookie(final HttpServletResponse response,
                                         final String refreshTokenValue,
                                         final ZonedDateTime refreshTokenExpiresAt) {
        if (!properties.getRefreshToken().isEnabled() || !StringUtils.hasText(refreshTokenValue)) {
            clearRefreshTokenCookie(response);
            return;
        }
        long maxAgeSeconds = Math.max(Duration.between(ZonedDateTime.now(refreshTokenExpiresAt.getZone()), refreshTokenExpiresAt).getSeconds(), 0);
        final var cookieProperties = properties.getRefreshToken().getCookie();
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(properties.getRefreshToken().getCookieName(), refreshTokenValue, maxAgeSeconds, cookieProperties).toString());
    }

    private void clearRefreshTokenCookie(final HttpServletResponse response) {
        final var cookieProperties = properties.getRefreshToken().getCookie();
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(properties.getRefreshToken().getCookieName(), "", 0, cookieProperties).toString());
    }

    private ResponseCookie buildCookie(final String name,
                                       final String tokenValue,
                                       final long maxAgeSeconds,
                                       final io.github.imaping.token.configuration.model.token.TokenCookieProperties cookieProperties) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, tokenValue)
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .maxAge(maxAgeSeconds);
        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }
        if (StringUtils.hasText(cookieProperties.getSameSite())) {
            builder.sameSite(cookieProperties.getSameSite());
        }
        return builder.build();
    }
}
