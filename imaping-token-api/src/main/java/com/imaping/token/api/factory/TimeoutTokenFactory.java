package com.imaping.token.api.factory;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.model.TimeoutAccessToken;

public interface TimeoutTokenFactory extends TokenFactory {

    TimeoutAccessToken create(Authentication authentication);
}
