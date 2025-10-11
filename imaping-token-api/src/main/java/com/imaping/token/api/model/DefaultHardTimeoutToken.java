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
public class DefaultHardTimeoutToken extends AbstractToken implements HardTimeoutToken {

    private static final long serialVersionUID = 6382497067285418560L;

    private String description;

    private String code;

    public DefaultHardTimeoutToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication, String code, String description) {
        super(id, expirationPolicy, authentication);
        this.code = code;
        this.description = description;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
