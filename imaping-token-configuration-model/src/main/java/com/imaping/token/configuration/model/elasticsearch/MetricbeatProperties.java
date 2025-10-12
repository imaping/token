package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Metricbeat 配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class MetricbeatProperties {

    /**
     * 服务器监控索引名称
     */
    private String indexName = "metricbeat-*";
}
