package com.imaping.token.resource.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(value = "dubhe.cloud.enabled", matchIfMissing = false, havingValue = "false")
public class ServletContextPathConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    public ServletContextPathConfig() {
    }

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        if (StringUtils.hasText(applicationName)) {
            factory.setContextPath("/" + applicationName);
        }
    }
}
