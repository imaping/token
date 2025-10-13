package com.imaping.token.core.model;

import lombok.Setter;

/**
 * @author miaoj
 */
@Setter
public class DefaultSecurityUserInfoContext implements SecurityUserInfoContext {

    private SecurityUserInfo securityUserInfo;

    public DefaultSecurityUserInfoContext() {
        securityUserInfo = new SecurityUserInfo(false);
    }

    @Override
    public SecurityUserInfo getCurrentUserInfo() {
        return securityUserInfo;
    }

}
