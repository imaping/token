package com.imaping.token.api.config;

import com.imaping.token.api.factory.*;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import com.imaping.token.api.authentication.TokenUserInfoContext;
import com.imaping.token.api.common.BeanCondition;
import com.imaping.token.api.common.BeanSupplier;
import com.imaping.token.api.expiration.builder.ExpirationPolicyBuilder;
import com.imaping.token.api.expiration.builder.HardTimeoutExpirationPolicyBuilder;
import com.imaping.token.api.expiration.builder.HardTimeoutExpirationPolicyDefaultBuilder;
import com.imaping.token.api.expiration.builder.TimeoutExpirationPolicyBuilder;
import com.imaping.token.api.generator.DefaultUniqueTokenIdGenerator;
import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import com.imaping.token.api.lock.LockRepository;
import com.imaping.token.api.model.HardTimeoutToken;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.CachingTokenRegistry;
import com.imaping.token.api.registry.DefaultTokenRegistry;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.core.TokenCoreAutoConfig;
import com.imaping.token.core.model.UserInfoContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
// 配置类顺序指定，保证 @ConditionalOnMissingBean 生效 保证 tokenUserInfoContext 注入正确
@AutoConfigureBefore(TokenCoreAutoConfig.class)
@Slf4j
public class TokenApiConfig {

    @ConditionalOnMissingBean(name = "tokenIdGenerator")
    @Bean
    public UniqueTokenIdGenerator tokenIdGenerator() {
        return new DefaultUniqueTokenIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(name = LockRepository.BEAN_NAME)
    public LockRepository tokenRegistryLockRepository(
            final ConfigurableApplicationContext applicationContext) throws Exception {
        return BeanSupplier.of(LockRepository.class)
                .when(BeanCondition.on("imaping.token.registry.core.enable-locking").isTrue().evenIfMissing().given(applicationContext.getEnvironment()))
                .supply(LockRepository::asDefault)
                .otherwise(LockRepository::noOp)
                .get();
    }

    @Bean
    public ExpirationPolicyBuilder<TimeoutAccessToken> accessTokenExpirationPolicy(final IMapingConfigurationProperties properties) {
        return new TimeoutExpirationPolicyBuilder(properties);
    }

    @Bean
    public HardTimeoutExpirationPolicyBuilder<HardTimeoutToken> hardTimeoutExpirationPolicy(final IMapingConfigurationProperties properties) {
        return new HardTimeoutExpirationPolicyDefaultBuilder<>(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "timeoutTokenFactory")
    public TimeoutTokenFactory timeoutTokenFactory(
            @Qualifier("tokenIdGenerator") final UniqueTokenIdGenerator idGenerator,
            @Qualifier("accessTokenExpirationPolicy") final ExpirationPolicyBuilder<TimeoutAccessToken> accessTokenExpirationPolicy) {
        return new TimeoutTokenDefaultFactory(idGenerator, accessTokenExpirationPolicy);
    }

    @Bean
    @ConditionalOnMissingBean(name = "hardTimeoutTokenFactory")
    public HardTimeoutTokenFactory hardTimeoutTokenFactory(
            @Qualifier("tokenIdGenerator") final UniqueTokenIdGenerator idGenerator,
            @Qualifier("hardTimeoutExpirationPolicy") final HardTimeoutExpirationPolicyBuilder<HardTimeoutToken> hardTimeoutExpirationPolicy) {
        return new HardTimeoutTokenDefaultFactory(idGenerator, hardTimeoutExpirationPolicy);
    }


    @ConditionalOnMissingBean(name = TokenFactory.BEAN_NAME)
    @Bean
    public TokenFactory defaultTokenFactory(final List<TokenFactory> factories) {
        DefaultTokenFactory parentFactory = new DefaultTokenFactory();
        factories.forEach(factory -> {
            parentFactory.addTokenFactory(factory.getTokenType(), factory);
        });
        return parentFactory;
    }

    @ConditionalOnMissingBean(name = TokenRegistry.BEAN_NAME)
    @Bean
    public TokenRegistry tokenRegistry(final IMapingConfigurationProperties properties) {
        log.info("Runtime memory is used as the persistence storage for retrieving and managing tokens. "
                + "Tokens that are issued during runtime will be LOST when the web server is restarted. This MAY impact SSO functionality.");
        val mem = properties.getToken().getRegistry().getInMemory();
        if (mem.isCache()) {
            return new CachingTokenRegistry();
        }
        val storageMap = new ConcurrentHashMap<String, Token>(mem.getInitialCapacity(), mem.getLoadFactor(), mem.getConcurrency());
        return new DefaultTokenRegistry(storageMap);
    }

    @Bean(name = UserInfoContext.BEAN_NAME)
    @ConditionalOnMissingBean(name = UserInfoContext.BEAN_NAME)
    public UserInfoContext tokenUserInfoContext() {
        return new TokenUserInfoContext();
    }
}
