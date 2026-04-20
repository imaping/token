package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.model.HardTimeoutToken;

public interface HardTimeoutTokenFactory extends TokenFactory {

    HardTimeoutToken create(Authentication<?> authentication, long timeToKillInSeconds, String code, String description);

    HardTimeoutToken create(Authentication<?> authentication);

}

