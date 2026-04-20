package io.github.imaping.token.api.refresh;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Access Token / Refresh Token 组合授权结果。
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenGrant implements Serializable {

    @Serial
    private static final long serialVersionUID = 2507606528217146263L;

    private final String tokenType;
    private final String accessToken;
    private final ZonedDateTime accessTokenExpiresAt;
    private final String refreshToken;
    private final ZonedDateTime refreshTokenExpiresAt;
}
