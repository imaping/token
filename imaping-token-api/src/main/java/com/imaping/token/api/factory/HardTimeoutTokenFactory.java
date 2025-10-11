package com.imaping.token.api.factory;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.model.HardTimeoutToken;

public interface HardTimeoutTokenFactory extends TokenFactory {

    HardTimeoutToken create(Authentication authentication, long timeToKillInSeconds, String code, String description);

    HardTimeoutToken create(Authentication authentication);

}