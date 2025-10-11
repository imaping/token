package com.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@Setter
@NoArgsConstructor
@Getter
public class DefaultTimeoutAccessToken extends AbstractToken implements TimeoutAccessToken {

    private static final long serialVersionUID = 5024818450360479885L;

    public DefaultTimeoutAccessToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication) {
        super(id, expirationPolicy, authentication);
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
