package com.imaping.token.configuration.model.token;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 调度任务配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("ScheduledJobProperties")
public class ScheduledJobProperties {

    /**
     * Scheduler settings to indicate how often the job should run.
     */
    @NestedConfigurationProperty
    private SchedulingProperties schedule = new SchedulingProperties();

    public ScheduledJobProperties(final String startDelay, final String repeatInterval) {
        schedule.setEnabled(true);
        schedule.setStartDelay(startDelay);
        schedule.setRepeatInterval(repeatInterval);
    }
}
