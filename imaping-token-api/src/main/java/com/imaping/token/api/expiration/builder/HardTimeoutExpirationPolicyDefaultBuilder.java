package com.imaping.token.api.expiration.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import com.imaping.token.api.model.HardTimeoutToken;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@Getter
public class HardTimeoutExpirationPolicyDefaultBuilder<T extends HardTimeoutToken> implements HardTimeoutExpirationPolicyBuilder<T> {

    private static final long serialVersionUID = -4105381841515569079L;

    private final IMapingConfigurationProperties properties;

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy() {
        return buildTokenExpirationPolicy(newDuration(properties.getToken().getAccessToken().getTimeToKillInSeconds()).getSeconds());
    }

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy(long timeToKillInSeconds) {
        return new HardTimeoutExpirationPolicy(timeToKillInSeconds);
    }
}
