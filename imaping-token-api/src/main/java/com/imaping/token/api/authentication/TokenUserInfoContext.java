package com.imaping.token.api.authentication;

import com.imaping.token.core.model.BaseUserInfo;
import com.imaping.token.core.model.UserInfo;
import com.imaping.token.core.model.UserInfoContext;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class TokenUserInfoContext implements UserInfoContext<String> {

    @Override
    public UserInfo<String> getCurrentUserInfo() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            //匿名用户
            return new UserInfo<>(false);
        } else {
            if (authentication instanceof DefaultTokenAuthentication) {
                DefaultTokenAuthentication tokenAuthentication = (DefaultTokenAuthentication) authentication;
                @SuppressWarnings("unchecked")
                final BaseUserInfo<String> baseUserInfo =
                        (BaseUserInfo<String>) tokenAuthentication.getAuthentication().getPrincipal().getUserInfo();
                final UserInfo<String> userInfo = new UserInfo<>(true);
                BeanUtils.copyProperties(baseUserInfo, userInfo, UserInfo.class);
                userInfo.setAccessToken(tokenAuthentication.getToken());
                return userInfo;
            }
            return new UserInfo<>(true);
        }
    }
}
