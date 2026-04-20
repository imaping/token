package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.model.RefreshToken;

public interface RefreshTokenFactory extends TokenFactory {

    RefreshToken create(Authentication<?> authentication, String accessTokenId);
}
