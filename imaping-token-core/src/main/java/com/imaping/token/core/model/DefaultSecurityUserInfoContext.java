package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public class DefaultSecurityUserInfoContext implements SecurityUserInfoContext {

    private SecurityUserInfo securityUserInfo;

    public DefaultSecurityUserInfoContext() {
        securityUserInfo = new SecurityUserInfo(false);
    }

    @Override
    public SecurityUserInfo getCurrentUserInfo() {
        return securityUserInfo;
    }

    public void setSecurityUserInfo(SecurityUserInfo securityUserInfo) {
        this.securityUserInfo = securityUserInfo;
    }
}
