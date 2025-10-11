package com.imaping.token.resource.client.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@ComponentScan(basePackages = {"dubhe.token.resource.client.aware"})
public class ResourceClientConfig {

    public ResourceClientConfig() {
    }


    @Bean
    @ConditionalOnProperty(value = "dubhe.cloud.enabled", matchIfMissing = false, havingValue = "false")
    public FilterRegistrationBean<?> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", corsConfiguration);
        FilterRegistrationBean<?> filterRegistrationBean = new FilterRegistrationBean<>(new CorsFilter(source));
        // 代表这个过滤器在众多过滤器中级别最高，也就是过滤的时候最先执行
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }
}
