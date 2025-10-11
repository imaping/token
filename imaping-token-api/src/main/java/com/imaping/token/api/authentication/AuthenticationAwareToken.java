package com.imaping.token.api.authentication;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.model.Token;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface AuthenticationAwareToken extends Token {
    Authentication getAuthentication();
}
