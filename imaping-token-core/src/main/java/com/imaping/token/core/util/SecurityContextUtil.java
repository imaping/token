package com.imaping.token.core.util;

import com.imaping.token.core.model.UserInfo;
import com.imaping.token.core.model.UserInfoContext;

import java.io.Serializable;

/**
 * 安全上下文
 *
 * @author miaoj
 */
public class SecurityContextUtil {

    private static UserInfoContext<?> userInfoContext;

    public void setUserInfoContext(UserInfoContext<?> userInfoContext) {
        SecurityContextUtil.userInfoContext = userInfoContext;
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息
     */
    @SuppressWarnings("unchecked")
    public static <ID extends Serializable> UserInfo<ID> getCurrentUserInfo() {
        // 通过泛型方法向调用方暴露具体 ID 类型,内部以通配符存储再做安全转换
        return (UserInfo<ID>) userInfoContext.getCurrentUserInfo();
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
