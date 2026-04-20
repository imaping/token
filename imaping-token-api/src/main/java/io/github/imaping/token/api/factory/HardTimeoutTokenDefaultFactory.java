package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.ExpirationPolicy;
import io.github.imaping.token.api.expiration.builder.HardTimeoutExpirationPolicyBuilder;
import io.github.imaping.token.api.generator.UniqueTokenIdGenerator;
import io.github.imaping.token.api.model.DefaultHardTimeoutToken;
import io.github.imaping.token.api.model.HardTimeoutToken;
import io.github.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class HardTimeoutTokenDefaultFactory implements HardTimeoutTokenFactory {

    protected final UniqueTokenIdGenerator idGenerator;

    protected final HardTimeoutExpirationPolicyBuilder<HardTimeoutToken> expirationPolicy;


    @Override
    public HardTimeoutToken create(Authentication<?> authentication, long timeToKillInSeconds, String code, String description) {
        final ExpirationPolicy tokenExpirationPolicy = expirationPolicy.buildTokenExpirationPolicy(timeToKillInSeconds);
        return new DefaultHardTimeoutToken(idGenerator.getNewTokenId(HardTimeoutToken.PREFIX), tokenExpirationPolicy, authentication, code, description);
    }

    @Override
    public HardTimeoutToken create(Authentication<?> authentication) {
        final ExpirationPolicy tokenExpirationPolicy = expirationPolicy.buildTokenExpirationPolicy();
        return new DefaultHardTimeoutToken(idGenerator.getNewTokenId(HardTimeoutToken.PREFIX), tokenExpirationPolicy, authentication, null, null);
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return HardTimeoutToken.class;
    }

}

