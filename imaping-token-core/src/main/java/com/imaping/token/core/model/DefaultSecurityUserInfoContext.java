package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public class DefaultSecurityUserInfoContext implements SecurityUserInfoContext<String> {

    private SecurityUserInfo<String> securityUserInfo;

    public DefaultSecurityUserInfoContext() {
        securityUserInfo = new SecurityUserInfo<>(false);
    }

    @Override
    public SecurityUserInfo<String> getCurrentUserInfo() {
        return securityUserInfo;
    }

    public void setSecurityUserInfo(SecurityUserInfo<String> securityUserInfo) {
        this.securityUserInfo = securityUserInfo;
    }
}
