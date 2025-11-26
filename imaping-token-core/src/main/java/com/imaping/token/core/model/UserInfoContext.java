package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public interface UserInfoContext<ID extends java.io.Serializable> {

    String BEAN_NAME = "userInfoContext";

    /**
     * 获取当前用户信息
     *
     * @return {@link UserInfo}
     */
    UserInfo<ID> getCurrentUserInfo();
}
