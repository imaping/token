package com.imaping.token.core.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 基础用户信息类 - 封装用户的基本身份和组织信息.
 *
 * <p><b>序列化要求:</b> 作为 Principal 的一部分,随 Token 一起存储在 Redis 中,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author miaoj
 * @see com.imaping.token.api.authentication.principal.Principal
 * @since 0.0.1
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "loginName")
public class BaseUserInfo implements Serializable {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * BaseUserInfo 对象作为 Token 的一部分存储在 Redis 中.
     */
    @Serial
    private static final long serialVersionUID = 8372445505086867962L;

    /**
     * 用户标识符
     */
    @JsonAlias({"id", "userId"})
    protected String id;

    /**
     * 当前登录用户的token
     */
    protected String accessToken;

    /**
     * 用户名称
     */
    protected String name;

    /**
     * 用户登录名
     */
    protected String loginName;

    /**
     * 用户角色
     */
    protected Set<String> roles;
}
