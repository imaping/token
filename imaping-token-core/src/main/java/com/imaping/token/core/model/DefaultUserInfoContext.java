package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public class DefaultUserInfoContext implements UserInfoContext {

    private UserInfo userInfo;

    public DefaultUserInfoContext() {
        this.userInfo = new UserInfo(false);
    }

    @Override
    public UserInfo getCurrentUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
