package com.imaping.token.api.expiration.builder;

import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.model.Token;

public interface HardTimeoutExpirationPolicyBuilder<T extends Token> extends ExpirationPolicyBuilder<T> {

    /**
     * Method build token expiration policy.
     *
     * @return - the policy
     */
    ExpirationPolicy buildTokenExpirationPolicy(long timeToKillInSeconds);
}
