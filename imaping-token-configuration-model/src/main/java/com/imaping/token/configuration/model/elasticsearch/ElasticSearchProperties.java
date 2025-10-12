package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * ElasticSearch 配置属性类 - 日志相关属性变量.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class ElasticSearchProperties {

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
