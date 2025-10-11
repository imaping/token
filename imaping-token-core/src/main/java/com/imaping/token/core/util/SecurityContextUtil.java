package com.imaping.token.core.util;

import com.imaping.token.core.model.UserInfo;
import com.imaping.token.core.model.UserInfoContext;

/**
 * 安全上下文
 *
 * @author miaoj
 */
public class SecurityContextUtil {

    private static UserInfoContext userInfoContext;

    public void setUserInfoContext(UserInfoContext userInfoContext) {
        SecurityContextUtil.userInfoContext = userInfoContext;
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息
     */
    public static UserInfo getCurrentUserInfo() {
        return userInfoContext.getCurrentUserInfo();
    }

    /**
     * 获取当前使用的token。
     *
     * @return 当前使用的token
     */
    public static String getCurrentToken() {
        return userInfoContext.getCurrentUserInfo().getAccessToken();
    }
}
