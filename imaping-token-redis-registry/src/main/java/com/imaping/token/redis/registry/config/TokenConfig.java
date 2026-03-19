package com.imaping.token.redis.registry.config;

import com.imaping.token.api.common.BeanCondition;
import com.imaping.token.api.common.BeanSupplier;
import com.imaping.token.api.config.TokenApiConfig;
import com.imaping.token.api.lock.DefaultLockRepository;
import com.imaping.token.api.lock.LockRepository;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.ConcurrentSessionControlTokenRegistry;
import com.imaping.token.api.registry.DefaultTokenRegistry;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.configuration.IMapingConfigurationProperties;
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
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
@ComponentScan(basePackages = {"com.imaping.token.redis.registry"})
@AutoConfigureBefore(value = {TokenApiConfig.class})
public class TokenConfig {

    private static final String LOCK_REGISTRY_KEY = "imaping.token.lock";

    private static final BeanCondition REDIS_CONDITION = BeanCondition.on("imaping.token.registry.redis.enabled").isTrue().evenIfMissing();

    private static final BeanCondition LOCKING_CONDITION = BeanCondition.on("imaping.token.registry.core.enable-locking").isTrue().evenIfMissing();

    @Bean
    public TokenRedisTemplate<String, Token> tokenRedisTemplate(final RedisConnectionFactory redisConnectionFactory) {
        return new DefaultTokenRedisTemplate<>(redisConnectionFactory);
    }

    @Bean(name = LockRepository.BEAN_NAME)
    @ConditionalOnMissingBean(name = LockRepository.BEAN_NAME)
    public LockRepository tokenRegistryLockRepository(final RedisConnectionFactory redisConnectionFactory,
                                                      final ConfigurableApplicationContext applicationContext) {
        return BeanSupplier.of(LockRepository.class)
                .when(REDIS_CONDITION.given(applicationContext.getEnvironment()))
                .and(LOCKING_CONDITION.given(applicationContext.getEnvironment()))
                .supply(() -> new DefaultLockRepository(new RedisLockRegistry(redisConnectionFactory, LOCK_REGISTRY_KEY)))
                .otherwise(() -> LOCKING_CONDITION.given(applicationContext.getEnvironment()).get()
                        ? LockRepository.asDefault()
                        : LockRepository.noOp())
                .get();
    }

    @Bean
    @ConditionalOnMissingBean(name = TokenRegistry.BEAN_NAME)
    public TokenRegistry tokenRegistry(@Qualifier("tokenRedisTemplate") final TokenRedisTemplate<String, Token> tokenRedisTemplate,
                                       @Qualifier(LockRepository.BEAN_NAME) final LockRepository lockRepository,
                                       final IMapingConfigurationProperties properties,
                                       final ConfigurableApplicationContext applicationContext) {

        final TokenRegistry delegate = BeanSupplier.of(TokenRegistry.class)
                .when(REDIS_CONDITION.given(applicationContext.getEnvironment()))
                .supply(() -> new RedisTokenRegistry(tokenRedisTemplate))
                .otherwise(DefaultTokenRegistry::new)
                .get();
        return new ConcurrentSessionControlTokenRegistry(
                delegate,
                lockRepository,
                properties.getToken().getRegistry().getConcurrentSessions());
    }

}
