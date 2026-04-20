package io.github.imaping.token.api.model;

import io.github.imaping.token.api.authentication.AuthenticationAwareToken;

/**
 * 用于续签 access token 的固定时长令牌。
 */
public interface RefreshToken extends AuthenticationAwareToken {
    String PREFIX = "RT";

    /**
     * 当前 refresh token 关联的 access token 标识。
     */
    String getAccessTokenId();
}
