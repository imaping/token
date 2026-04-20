package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.ExpirationPolicy;
import io.github.imaping.token.api.expiration.builder.ExpirationPolicyBuilder;
import io.github.imaping.token.api.generator.UniqueTokenIdGenerator;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class TimeoutTokenDefaultFactory implements TimeoutTokenFactory {

    protected final UniqueTokenIdGenerator idGenerator;

    protected final ExpirationPolicyBuilder<TimeoutAccessToken> expirationPolicy;

    @Override
    public TimeoutAccessToken create(Authentication<?> authentication) {
        final ExpirationPolicy tokenExpirationPolicy = expirationPolicy.buildTokenExpirationPolicy();
        return new DefaultTimeoutAccessToken(idGenerator.getNewTokenId(TimeoutAccessToken.PREFIX), tokenExpirationPolicy, authentication);
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return TimeoutAccessToken.class;
    }
}

