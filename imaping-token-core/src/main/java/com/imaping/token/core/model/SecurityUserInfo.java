package com.imaping.token.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 用户信息
 *
 * @author By miaoj
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SecurityUserInfo extends UserInfo {

    private static final long serialVersionUID = -617454775891099862L;

    /**
     * 是否超管
     */
    private boolean admin = false;

    /**
     * 初始化实例。
     *
     * @param authenticated 是否验证（即登录）
     */
    public SecurityUserInfo(boolean authenticated) {
        this.admin = false;
        this.authenticated = authenticated;
    }
}
