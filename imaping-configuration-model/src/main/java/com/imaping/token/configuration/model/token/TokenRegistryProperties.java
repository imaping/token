package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class TokenRegistryProperties implements Serializable {
    private static final long serialVersionUID = -2805075446340583131L;

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
