package com.imaping.token.api.expiration.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.TimeoutExpirationPolicy;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.configuration.DubheConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@Getter
public class TimeoutExpirationPolicyBuilder implements ExpirationPolicyBuilder<TimeoutAccessToken> {

    private static final long serialVersionUID = 4176329929374375103L;

    private final DubheConfigurationProperties properties;

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy() {
        return new TimeoutExpirationPolicy(newDuration(properties.getToken().getAccessToken().getTimeToKillInSeconds()).getSeconds());
    }
}
