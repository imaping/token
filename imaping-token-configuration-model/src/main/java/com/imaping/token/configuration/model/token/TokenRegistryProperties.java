package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Token 注册表配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenRegistryProperties {

    /**
     * Token registry cleaner settings.
     */
    @NestedConfigurationProperty
    private ScheduledJobProperties cleaner = new ScheduledJobProperties("PT10S", "PT1M");

    @NestedConfigurationProperty
    private TokenRegistryCoreProperties core = new TokenRegistryCoreProperties();

    /**
     * Settings relevant for the default in-memory token registry.
     */
    @NestedConfigurationProperty
    private InMemoryTokenRegistryProperties inMemory = new InMemoryTokenRegistryProperties();

    /**
     * Redis registry settings.
     */
    @NestedConfigurationProperty
    private RedisTokenRegistryProperties redis = new RedisTokenRegistryProperties();
}
