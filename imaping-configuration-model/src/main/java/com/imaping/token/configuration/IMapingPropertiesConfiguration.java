package com.imaping.token.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author miaoj
 */
@Configuration
@EnableConfigurationProperties(value = {IMapingConfigurationProperties.class})
public class IMapingPropertiesConfiguration {
}
