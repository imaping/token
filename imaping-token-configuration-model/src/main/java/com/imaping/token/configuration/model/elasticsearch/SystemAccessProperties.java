package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 系统访问配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class SystemAccessProperties {

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
