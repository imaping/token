package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.model.TimeoutAccessToken;

public interface TimeoutTokenFactory extends TokenFactory {

    TimeoutAccessToken create(Authentication<?> authentication);
}

