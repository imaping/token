package com.imaping.token.configuration.model.district;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 行政区配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class DistrictProperties {

    /**
     * 配置文件数据源所属行政区
     */
    private String defaultCode;

    /**
     * 是否启用严格模式
     * 用户行政区为空抛出异常
     * 行政区未配置数据源抛出异常
     */
    private boolean strict = false;
}
