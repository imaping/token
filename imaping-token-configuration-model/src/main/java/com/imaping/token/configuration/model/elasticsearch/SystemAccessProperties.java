package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class SystemAccessProperties implements Serializable {

    private static final long serialVersionUID = -2707612028826747444L;
    /**
     * 索引名称
     */
    private String indexName = "system-access-*";

    /**
     * 用户字段名称
     */
    private String userNameFieldName = "userName";

    /**
     * 菜单id字段名称
     */
    private String menuIdFieldName = "menuId";

    /**
     * 部门id字段名称
     */
    private String deptIdFieldName = "deptId";


    private String productCodeFieldName = "productCode";

    private String productCategoryCodeFieldName = "productCategoryCode";
}
