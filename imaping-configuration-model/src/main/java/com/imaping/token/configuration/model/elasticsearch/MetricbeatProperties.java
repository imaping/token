package com.imaping.token.configuration.model.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class MetricbeatProperties  implements Serializable {
    private static final long serialVersionUID = 6799851437642829017L;
    /**
     * 服务器监控索引名称
     */
    private String indexName = "metricbeat-*";
}
