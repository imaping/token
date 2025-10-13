package com.imaping.token.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.util.StringUtils;

import java.io.Serial;

/**
 * 用户信息类 - 扩展基础用户信息,包含认证状态和访问令牌.
 *
 * <p><b>序列化要求:</b> 继承自 BaseUserInfo,作为 Token 的一部分存储在 Redis 中,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author miaoj
 * @since 0.0.1
 * @see BaseUserInfo
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserInfo extends BaseUserInfo {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * UserInfo 对象作为 Token 的一部分存储在 Redis 中.
     */
    @Serial
    private static final long serialVersionUID = -7249321741539317321L;

    /**
     * 用户是否已认证
     */
    protected boolean authenticated;

    /**
     * 此用户是否为匿名用户
     *
     * @return 是否为匿名用户
     */
    public boolean isAnonymous() {
        return !authenticated && StringUtils.isEmpty(loginName);
    }
 }
