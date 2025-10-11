package com.imaping.token.redis.registry.config;

import com.imaping.token.api.common.BeanCondition;
import com.imaping.token.api.common.BeanSupplier;
import com.imaping.token.api.config.TokenApiConfig;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.DefaultTokenRegistry;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.redis.registry.DefaultTokenRedisTemplate;
import com.imaping.token.redis.registry.RedisTokenRegistry;
import com.imaping.token.redis.registry.TokenRedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ComponentScan(basePackages = {"com.imaping.token.redis.registry"})
@AutoConfigureBefore(value = {TokenApiConfig.class})
public class TokenConfig {

    private static final BeanCondition CONDITION = BeanCondition.on("imaping.token.registry.redis.enabled").isTrue().evenIfMissing();


    @Bean
    public TokenRedisTemplate<String, Token> tokenRedisTemplate(final RedisConnectionFactory redisConnectionFactory) {
        return new DefaultTokenRedisTemplate<>(redisConnectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = TokenRegistry.BEAN_NAME)
    public TokenRegistry tokenRegistry(@Qualifier("tokenRedisTemplate") final TokenRedisTemplate<String, Token> tokenRedisTemplate,
                                       final ConfigurableApplicationContext applicationContext) {

        return BeanSupplier.of(TokenRegistry.class)
                .when(CONDITION.given(applicationContext.getEnvironment()))
                .supply(() -> new RedisTokenRegistry(tokenRedisTemplate))
                .otherwise(DefaultTokenRegistry::new)
                .get();
    }

}
