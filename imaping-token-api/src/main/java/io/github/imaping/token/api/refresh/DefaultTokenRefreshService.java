package io.github.imaping.token.api.refresh;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.exception.TokenAuthenticationException;
import io.github.imaping.token.api.exception.TokenError;
import io.github.imaping.token.api.factory.RefreshTokenFactory;
import io.github.imaping.token.api.factory.TimeoutTokenFactory;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;

/**
 * 默认的 refresh token 续签服务。
 */
@RequiredArgsConstructor
public class DefaultTokenRefreshService implements TokenRefreshService {

    private final TokenRegistry tokenRegistry;
    private final TimeoutTokenFactory timeoutTokenFactory;
    private final RefreshTokenFactory refreshTokenFactory;
    private final IMapingTokenConfigurationProperties properties;
    private final AccessTokenCodec accessTokenCodec;

    @Override
    public boolean isEnabled() {
        return properties.getRefreshToken().isEnabled();
    }

    @Override
    public TokenGrant issue(final Authentication<?> authentication) throws Exception {
        TimeoutAccessToken accessToken = timeoutTokenFactory.create(authentication);
        tokenRegistry.addToken(accessToken);

        RefreshToken refreshToken = null;
        if (isEnabled()) {
            refreshToken = refreshTokenFactory.create(authentication, accessToken.getId());
            tokenRegistry.addToken(refreshToken);
        }
        return buildGrant(accessToken, refreshToken, accessTokenCodec.encode(accessToken));
    }

    @Override
    public TokenGrant refresh(final String refreshTokenId) throws Exception {
        if (!isEnabled()) {
            throw invalidRequest("Refresh token 功能未启用");
        }
        if (!StringUtils.hasText(refreshTokenId)) {
            throw invalidRequest("refreshToken 不能为空");
        }
        Token storedToken = tokenRegistry.getToken(refreshTokenId);
        if (!(storedToken instanceof RefreshToken refreshToken) || refreshToken.isExpired()) {
            if (storedToken != null) {
                tokenRegistry.deleteToken(storedToken.getId());
            }
            throw invalidToken("Refresh token 已失效");
        }
        final Authentication<?> authentication = refreshToken.getAuthentication();
        if (!StringUtils.hasText(refreshToken.getAccessTokenId())) {
            throw invalidToken("Refresh token 缺少 access token 关联关系");
        }

        tokenRegistry.deleteToken(refreshToken.getAccessTokenId());
        tokenRegistry.deleteToken(refreshToken.getId());
        return issue(authentication);
    }

    @Override
    public long revokeGrant(final String refreshTokenId) throws Exception {
        if (!StringUtils.hasText(refreshTokenId)) {
            return 0;
        }
        Token storedToken = tokenRegistry.getToken(refreshTokenId);
        if (!(storedToken instanceof RefreshToken refreshToken)) {
            return 0;
        }
        long deleted = tokenRegistry.deleteToken(refreshToken.getId());
        if (StringUtils.hasText(refreshToken.getAccessTokenId())) {
            deleted += tokenRegistry.deleteToken(refreshToken.getAccessTokenId());
        }
        return deleted;
    }

    private TokenGrant buildGrant(final TimeoutAccessToken accessToken, final RefreshToken refreshToken, final String accessTokenValue) {
        return TokenGrant.builder()
                .tokenType("Bearer")
                .accessToken(accessTokenValue)
                .accessTokenExpiresAt(resolveExpirationTime(accessToken))
                .refreshToken(refreshToken != null ? refreshToken.getId() : null)
                .refreshTokenExpiresAt(resolveExpirationTime(refreshToken))
                .build();
    }

    private ZonedDateTime resolveExpirationTime(final Token token) {
        if (token == null || token.getExpirationPolicy() == null) {
            return null;
        }
        long timeToLive = token.getExpirationPolicy().getTimeToLive() == null ? 0 : token.getExpirationPolicy().getTimeToLive();
        long timeToIdle = token.getExpirationPolicy().getTimeToIdle() == null ? 0 : token.getExpirationPolicy().getTimeToIdle();
        if (timeToLive > 0 && timeToIdle > 0) {
            return token.getCreationTime().plusSeconds(timeToLive)
                    .isBefore(token.getLastTimeUsed().plusSeconds(timeToIdle))
                    ? token.getCreationTime().plusSeconds(timeToLive)
                    : token.getLastTimeUsed().plusSeconds(timeToIdle);
        }
        if (timeToLive > 0) {
            return token.getCreationTime().plusSeconds(timeToLive);
        }
        if (timeToIdle > 0) {
            return token.getLastTimeUsed().plusSeconds(timeToIdle);
        }
        return null;
    }

    private TokenAuthenticationException invalidRequest(final String message) {
        return new TokenAuthenticationException(new TokenError(TokenError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message));
    }

    private TokenAuthenticationException invalidToken(final String message) {
        return new TokenAuthenticationException(new TokenError(TokenError.INVALID_TOKEN, HttpStatus.UNAUTHORIZED, message));
    }
}
