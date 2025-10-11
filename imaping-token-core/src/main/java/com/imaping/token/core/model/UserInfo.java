package com.imaping.token.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.util.StringUtils;

/**
 * 当前认证的用户信息。
 *
 * @author miaoj
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserInfo extends BaseUserInfo {

    private static final long serialVersionUID = -7249321741539317321L;
    /**
     * 当前登录用户的token
     */
    protected String accessToken;

    /**
     * 用户是否已认证
     */
    protected boolean authenticated;


    /**
     * 初始化实例。
     *
     * @param authenticated 是否验证（即登录）
     */
    public UserInfo(boolean authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * 此用户是否为匿名用户
     *
     * @return 是否为匿名用户
     */
    public boolean isAnonymous() {
        return !authenticated && StringUtils.isEmpty(loginName);
    }
 }
