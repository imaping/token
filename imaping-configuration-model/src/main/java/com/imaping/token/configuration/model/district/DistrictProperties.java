package com.imaping.token.configuration.model.district;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class DistrictProperties implements Serializable {

    private static final long serialVersionUID = -2240708805068979618L;

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
