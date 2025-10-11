package com.imaping.token.core.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

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
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "loginName")
public class BaseUserInfo implements Serializable {

    private static final long serialVersionUID = 8372445505086867962L;

    /**
     * 用户标识符
     */
    @JsonAlias({"id", "userId"})
    protected String id;

    /**
     * 用户名称
     */
    protected String name;

    /**
     * 用户登录名
     */
    protected String loginName;

    /**
     * 用户所属部门id
     */
    protected String departmentId;

    /**
     * 用户所属部门名称
     */
    protected String departmentName;

    /**
     * 用户所属组织机构id
     */
    protected String organizationId;

    /**
     * 用户所属组织机构名称
     */
    protected String organizationName;

    /**
     * 用户所属行政区编码
     */
    protected String districtCode;

    /**
     * 用户所属行政区名称
     */
    protected String districtName;
}
