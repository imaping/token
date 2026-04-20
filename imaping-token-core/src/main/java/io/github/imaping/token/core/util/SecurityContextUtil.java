package io.github.imaping.token.core.util;

import io.github.imaping.token.core.model.UserInfo;
import io.github.imaping.token.core.model.UserInfoContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    /**
     * 获取当前访问令牌在注册表中的标识。
     *
     * <p>在 JWT 模式下,该值与客户端看到的 JWT 字符串不同。</p>
     */
    public static String getCurrentTokenId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            try {
                final Object tokenId = authentication.getClass().getMethod("getTokenId").invoke(authentication);
                if (tokenId != null) {
                    return String.valueOf(tokenId);
                }
            } catch (ReflectiveOperationException ignored) {
                // 兼容非 token 认证场景,回退到原始 token 值
            }
        }
        return getCurrentToken();
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    public static String getUserId() {
        final UserInfo<?> currentUserInfo = userInfoContext.getCurrentUserInfo();
        return currentUserInfo != null && currentUserInfo.getId() != null
                ? String.valueOf(currentUserInfo.getId())
                : null;
    }
}

