package com.imaping.token.api.factory;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.builder.ExpirationPolicyBuilder;
import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import com.imaping.token.api.model.DefaultTimeoutAccessToken;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class TimeoutTokenDefaultFactory implements TimeoutTokenFactory {

    protected final UniqueTokenIdGenerator idGenerator;

    protected final ExpirationPolicyBuilder<TimeoutAccessToken> expirationPolicy;

    @Override
    public TimeoutAccessToken create(Authentication authentication) {
        final ExpirationPolicy tokenExpirationPolicy = expirationPolicy.buildTokenExpirationPolicy();
        return new DefaultTimeoutAccessToken(idGenerator.getNewTokenId(TimeoutAccessToken.PREFIX), tokenExpirationPolicy, authentication);
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return TimeoutAccessToken.class;
    }
}
