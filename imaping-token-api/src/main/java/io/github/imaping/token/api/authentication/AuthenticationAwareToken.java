package io.github.imaping.token.api.authentication;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.imaping.token.api.model.Token;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface AuthenticationAwareToken extends Token {
    Authentication<?> getAuthentication();
}

