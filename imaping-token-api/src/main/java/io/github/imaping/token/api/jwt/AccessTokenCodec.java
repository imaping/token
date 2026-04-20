package io.github.imaping.token.api.jwt;

import io.github.imaping.token.api.model.Token;

public interface AccessTokenCodec {

    String BEAN_NAME = "accessTokenCodec";

    boolean isJwtEnabled();

    String encode(Token token);

    DecodedAccessToken decode(String tokenValue);
}
