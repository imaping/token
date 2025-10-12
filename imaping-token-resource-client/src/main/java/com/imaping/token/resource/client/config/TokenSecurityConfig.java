package com.imaping.token.resource.client.config;

import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import com.imaping.token.resource.client.authentication.TokenAuthenticationEntryPoint;
import com.imaping.token.resource.client.authentication.TokenAuthenticationProvider;
import com.imaping.token.resource.client.filter.TokenAuthenticationFilter;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@Getter
@ConditionalOnMissingBean(TokenSecurityConfig.class)
public class TokenSecurityConfig {
    private final TokenRegistry tokenRegistry;

    private final TokenFactory tokenFactory;

    private final IMapingConfigurationProperties properties;


    public TokenSecurityConfig(TokenRegistry tokenRegistry, @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory, IMapingConfigurationProperties properties) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
        this.properties = properties;
    }

    @Bean
    public TokenAuthenticationProvider tokenAuthenticationProvider() {
        return new TokenAuthenticationProvider(tokenRegistry);
    }

    @Bean
    public TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint() {
        return new TokenAuthenticationEntryPoint();
    }

    @Bean
    @Order(2)
    @ConditionalOnMissingBean(name = "imapingApiFilterChain")
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(tokenAuthenticationProvider())
                .httpBasic(configurer -> configurer.authenticationEntryPoint(tokenAuthenticationEntryPoint()))
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityMatcher(getAllAntMatchers())
                .authorizeHttpRequests(registry -> {
                    if (!CollectionUtils.isEmpty(getAuthenticatedAntMatchersWithMethod())) {
                        getAuthenticatedAntMatchersWithMethod().forEach((method, antPatterns) -> {
                            registry.requestMatchers(method, antPatterns).authenticated();
                        });
                    }
                    if (!ArrayUtils.isEmpty(getAuthenticatedAntMatchers())) {
                        registry.requestMatchers(getAuthenticatedAntMatchers()).authenticated();
                    }
                    if (!ArrayUtils.isEmpty(getPermitAntMatchers())) {
                        registry.requestMatchers(getPermitAntMatchers()).permitAll();
                    }
                    if (!CollectionUtils.isEmpty(getPermitAntMatchersWithMethod())) {
                        getPermitAntMatchersWithMethod().forEach((method, antPatterns) -> {
                            registry.requestMatchers(method, antPatterns).permitAll();
                        });
                    }
                    if (isAnyRequestAuthenticated()) {
                        registry.anyRequest().authenticated();
                    }
                })
                .logout(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .with(AuthenticationFilterDsl.custom(tokenAuthenticationEntryPoint(), properties), customizer -> {});
        configure(http);
        return http.build();
    }

    protected void configure(HttpSecurity http) {
    }

    protected boolean isAnyRequestAuthenticated() {
        return true;
    }

    /**
     * 获取范围
     */
    protected String[] getAllAntMatchers() {
        return new String[]{"/**"};
    }

    /**
     * 不需要授权 AntMatchers
     */
    protected String[] getPermitAntMatchers() {
        return null;
    }

    /**
     * 需要授权 AntMatchers
     */
    protected String[] getAuthenticatedAntMatchers() {
        return null;
    }


    /**
     * 不需要授权 AntMatchers with method
     */
    protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
        Map<HttpMethod, String[]> permitMap = new HashMap<>();
        if (!properties.getCloud().isEnabled()) {
            permitMap.put(HttpMethod.OPTIONS, new String[]{"/**"});
        }
        permitMap.put(HttpMethod.GET, new String[]{"/**"});
        return permitMap;
    }

    /**
     * 需要授权 AntMatchers with method
     */
    protected Map<HttpMethod, String[]> getAuthenticatedAntMatchersWithMethod() {
        return null;
    }

    public static class AuthenticationFilterDsl extends AbstractHttpConfigurer<AuthenticationFilterDsl, HttpSecurity> {

        private final TokenAuthenticationEntryPoint entryPoint;
        private final IMapingConfigurationProperties properties;

        public AuthenticationFilterDsl(TokenAuthenticationEntryPoint entryPoint, IMapingConfigurationProperties properties) {
            this.entryPoint = entryPoint;
            this.properties = properties;
        }

        @Override
        public void configure(HttpSecurity http) {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
            http.addFilterBefore(new TokenAuthenticationFilter(authenticationManager, entryPoint, properties), UsernamePasswordAuthenticationFilter.class);
        }

        public static AuthenticationFilterDsl custom(TokenAuthenticationEntryPoint entryPoint, IMapingConfigurationProperties properties) {
            return new AuthenticationFilterDsl(entryPoint, properties);
        }
    }
}
