package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import io.github.imaping.token.api.expiration.ExpirationPolicy;
import io.github.imaping.token.api.expiration.builder.ExpirationPolicyBuilder;
import io.github.imaping.token.api.generator.UniqueTokenIdGenerator;
import io.github.imaping.token.api.model.DefaultJwtAccessToken;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class TimeoutTokenDefaultFactory implements TimeoutTokenFactory {

    protected final UniqueTokenIdGenerator idGenerator;

    protected final ExpirationPolicyBuilder<TimeoutAccessToken> expirationPolicy;
    protected final IMapingTokenConfigurationProperties properties;

    @Override
    public TimeoutAccessToken create(Authentication<?> authentication) {
        if (properties.getAccessToken().isCreateAsJwt()) {
            return new DefaultJwtAccessToken(
                    idGenerator.getNewTokenId(TimeoutAccessToken.PREFIX),
                    new HardTimeoutExpirationPolicy(resolveJwtDuration().getSeconds()),
                    authentication
            );
        }
        final ExpirationPolicy tokenExpirationPolicy = expirationPolicy.buildTokenExpirationPolicy();
        return new DefaultTimeoutAccessToken(idGenerator.getNewTokenId(TimeoutAccessToken.PREFIX), tokenExpirationPolicy, authentication);
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return TimeoutAccessToken.class;
    }

    private Duration resolveJwtDuration() {
        return expirationPolicy.newDuration(properties.getAccessToken().getTimeToKillInSeconds());
    }
}

