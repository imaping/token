package io.github.imaping.token.api.expiration.builder;

import io.github.imaping.token.api.expiration.ExpirationPolicy;
import io.github.imaping.token.api.model.Token;

public interface HardTimeoutExpirationPolicyBuilder<T extends Token> extends ExpirationPolicyBuilder<T> {

    /**
     * Method build token expiration policy.
     *
     * @return - the policy
     */
    ExpirationPolicy buildTokenExpirationPolicy(long timeToKillInSeconds);
}

