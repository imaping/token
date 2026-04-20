package io.github.imaping.token.api.jwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 访问令牌解码结果。
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DecodedAccessToken {

    private final String tokenValue;
    private final String tokenId;
    private final boolean jwt;

    public static DecodedAccessToken plain(final String tokenValue) {
        return new DecodedAccessToken(tokenValue, tokenValue, false);
    }

    public static DecodedAccessToken jwt(final String tokenValue, final String tokenId) {
        return new DecodedAccessToken(tokenValue, tokenId, true);
    }
}
