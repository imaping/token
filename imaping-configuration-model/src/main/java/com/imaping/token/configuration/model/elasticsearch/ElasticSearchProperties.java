package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

/**
 * 日志相关属性变量
 *
 */
@Getter
@Setter
@Accessors(chain = true)
public class ElasticSearchProperties  implements Serializable {

    private static final long serialVersionUID = -3574936602826335893L;
    /**
     * elasticsearch节点
     */
    private String[] hosts;

    private String username;

    private String password;

    @NestedConfigurationProperty
    private MetricbeatProperties metricbeat = new MetricbeatProperties();

    @NestedConfigurationProperty
    private SystemAccessProperties systemAccess = new SystemAccessProperties();
}
