package io.github.imaping.token.resource.client.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"io.github.imaping.token.resource.client.aware"})
public class ResourceClientConfig {
}

