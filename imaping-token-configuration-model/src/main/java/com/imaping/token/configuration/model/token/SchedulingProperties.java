package com.imaping.token.configuration.model.token;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 调度配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonFilter("SchedulingProperties")
public class SchedulingProperties {

    /**
     * Whether scheduler should be enabled to schedule the job to run.
     */
    private boolean enabled = true;

    /**
     * Overrides {@link SchedulingProperties#enabled} property value of true
     * if this property does not match hostname of CAS server.
     * This can be useful if deploying CAS with an image in a statefulset
     * where all names are predictable but
     * where having different configurations for different servers
     * is hard. The value can be an exact hostname
     * or a regular expression that will be used to match the hostname.
     */
    private String enabledOnHost = ".*";

    /**
     * String representation of a start delay of loading data for a data store implementation.
     * This is the delay between scheduler startup and first job’s execution
     */
    private String startDelay = "PT15S";

    /**
     * String representation of a repeat interval of re-loading data for an data store implementation.
     * This is the timeout between consecutive job’s executions.
     */
    private String repeatInterval = "PT2M";
}
