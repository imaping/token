package com.imaping.token.core.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 安全用户信息类 - 扩展用户信息,包含安全相关属性(如管理员标识).
 *
 * <p><b>序列化要求:</b> 继承自 UserInfo,作为 Token 的一部分存储在 Redis 中,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author By miaoj
 * @since 0.0.1
 * @see UserInfo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SecurityUserInfo extends UserInfo {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * SecurityUserInfo 对象作为 Token 的一部分存储在 Redis 中.
     */
    @Serial
    private static final long serialVersionUID = -617454775891099862L;

    /**
     * 是否超管
     */
    @Builder.Default
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
