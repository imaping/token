package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public class DefaultUserInfoContext implements UserInfoContext<String> {

    private UserInfo<String> userInfo;

    public DefaultUserInfoContext() {
        this.userInfo = new UserInfo<>(false);
    }

    @Override
    public UserInfo<String> getCurrentUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo<String> userInfo) {
        this.userInfo = userInfo;
    }
}
