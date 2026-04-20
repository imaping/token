package io.github.imaping.token.api.config;

import io.github.imaping.token.api.factory.*;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.api.authentication.TokenUserInfoContext;
import io.github.imaping.token.api.common.BeanCondition;
import io.github.imaping.token.api.common.BeanSupplier;
import io.github.imaping.token.api.expiration.builder.ExpirationPolicyBuilder;
import io.github.imaping.token.api.expiration.builder.HardTimeoutExpirationPolicyBuilder;
import io.github.imaping.token.api.expiration.builder.HardTimeoutExpirationPolicyDefaultBuilder;
import io.github.imaping.token.api.expiration.builder.TimeoutExpirationPolicyBuilder;
import io.github.imaping.token.api.generator.DefaultUniqueTokenIdGenerator;
import io.github.imaping.token.api.generator.UniqueTokenIdGenerator;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.jwt.DefaultAccessTokenCodec;
import io.github.imaping.token.api.lock.LockRepository;
import io.github.imaping.token.api.model.HardTimeoutToken;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.registry.CachingTokenRegistry;
import io.github.imaping.token.api.registry.ConcurrentSessionControlTokenRegistry;
import io.github.imaping.token.api.registry.DefaultTokenRegistry;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.api.refresh.DefaultTokenRefreshService;
import io.github.imaping.token.api.refresh.TokenRefreshService;
import io.github.imaping.token.api.session.DefaultTokenSessionService;
import io.github.imaping.token.api.session.TokenSessionService;
import io.github.imaping.token.core.TokenCoreAutoConfig;
import io.github.imaping.token.core.model.UserInfoContext;
import io.github.imaping.token.configuration.model.token.TokenJwtProperties;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
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
    public ExpirationPolicyBuilder<TimeoutAccessToken> accessTokenExpirationPolicy(final IMapingTokenConfigurationProperties properties) {
        return new TimeoutExpirationPolicyBuilder(properties);
    }

    @Bean
    public HardTimeoutExpirationPolicyBuilder<HardTimeoutToken> hardTimeoutExpirationPolicy(final IMapingTokenConfigurationProperties properties) {
        return new HardTimeoutExpirationPolicyDefaultBuilder<>(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "timeoutTokenFactory")
    public TimeoutTokenFactory timeoutTokenFactory(
            @Qualifier("tokenIdGenerator") final UniqueTokenIdGenerator idGenerator,
            @Qualifier("accessTokenExpirationPolicy") final ExpirationPolicyBuilder<TimeoutAccessToken> accessTokenExpirationPolicy,
            final IMapingTokenConfigurationProperties properties) {
        return new TimeoutTokenDefaultFactory(idGenerator, accessTokenExpirationPolicy, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "hardTimeoutTokenFactory")
    public HardTimeoutTokenFactory hardTimeoutTokenFactory(
            @Qualifier("tokenIdGenerator") final UniqueTokenIdGenerator idGenerator,
            @Qualifier("hardTimeoutExpirationPolicy") final HardTimeoutExpirationPolicyBuilder<HardTimeoutToken> hardTimeoutExpirationPolicy) {
        return new HardTimeoutTokenDefaultFactory(idGenerator, hardTimeoutExpirationPolicy);
    }

    @Bean
    @ConditionalOnMissingBean(name = "refreshTokenFactory")
    public RefreshTokenFactory refreshTokenFactory(
            @Qualifier("tokenIdGenerator") final UniqueTokenIdGenerator idGenerator,
            final IMapingTokenConfigurationProperties properties) {
        return new RefreshTokenDefaultFactory(idGenerator, properties);
    }


    @ConditionalOnMissingBean(name = TokenFactory.BEAN_NAME)
    @Bean
    public TokenFactory defaultTokenFactory(final List<TokenFactory> factories) {
        DefaultTokenFactory parentFactory = new DefaultTokenFactory();
        factories.forEach(factory -> parentFactory.addTokenFactory(factory.getTokenType(), factory));
        return parentFactory;
    }

    @ConditionalOnMissingBean(name = TokenRegistry.BEAN_NAME)
    @Bean
    public TokenRegistry tokenRegistry(final IMapingTokenConfigurationProperties properties,
                                       @Qualifier(LockRepository.BEAN_NAME) final LockRepository lockRepository) {
        log.info("Runtime memory is used as the persistence storage for retrieving and managing tokens. "
                + "Tokens that are issued during runtime will be LOST when the web server is restarted. This MAY impact SSO functionality.");
        val mem = properties.getRegistry().getInMemory();
        final TokenRegistry delegate = mem.isCache()
                ? new CachingTokenRegistry()
                : new DefaultTokenRegistry(new ConcurrentHashMap<>(mem.getInitialCapacity(), mem.getLoadFactor(), mem.getConcurrency()));
        return new ConcurrentSessionControlTokenRegistry(
                delegate,
                lockRepository,
                properties.getRegistry().getConcurrentSessions());
    }

    @Bean(name = UserInfoContext.BEAN_NAME)
    @ConditionalOnMissingBean(name = UserInfoContext.BEAN_NAME)
    public UserInfoContext<String> tokenUserInfoContext() {
        return new TokenUserInfoContext();
    }

    @Bean(name = TokenSessionService.BEAN_NAME)
    @ConditionalOnMissingBean(name = TokenSessionService.BEAN_NAME)
    public TokenSessionService tokenSessionService(@Qualifier(TokenRegistry.BEAN_NAME) final TokenRegistry tokenRegistry) {
        return new DefaultTokenSessionService(tokenRegistry);
    }

    @Bean(name = AccessTokenCodec.BEAN_NAME)
    @ConditionalOnMissingBean(name = AccessTokenCodec.BEAN_NAME)
    public AccessTokenCodec accessTokenCodec(final IMapingTokenConfigurationProperties properties) {
        return new DefaultAccessTokenCodec(properties);
    }

    @Bean
    public Runnable jwtConfigurationValidator(final IMapingTokenConfigurationProperties properties) {
        validateJwtConfiguration(properties);
        return () -> {
        };
    }

    @Bean(name = TokenRefreshService.BEAN_NAME)
    @ConditionalOnMissingBean(name = TokenRefreshService.BEAN_NAME)
    public TokenRefreshService tokenRefreshService(@Qualifier(TokenRegistry.BEAN_NAME) final TokenRegistry tokenRegistry,
                                                   final TimeoutTokenFactory timeoutTokenFactory,
                                                   final RefreshTokenFactory refreshTokenFactory,
                                                   final IMapingTokenConfigurationProperties properties,
                                                   @Qualifier(AccessTokenCodec.BEAN_NAME) final AccessTokenCodec accessTokenCodec) {
        return new DefaultTokenRefreshService(tokenRegistry, timeoutTokenFactory, refreshTokenFactory, properties, accessTokenCodec);
    }

    private void validateJwtConfiguration(final IMapingTokenConfigurationProperties properties) {
        if (!properties.getAccessToken().isCreateAsJwt()) {
            return;
        }
        final TokenJwtProperties jwtProperties = properties.getAccessToken().getJwt();
        if (!StringUtils.hasText(jwtProperties.getIssuer())) {
            throw new IllegalStateException("启用 JWT AccessToken 时必须配置非空的 imaping.token.accessToken.jwt.issuer");
        }
        if (!StringUtils.hasText(jwtProperties.getAudience())) {
            throw new IllegalStateException("启用 JWT AccessToken 时必须配置非空的 imaping.token.accessToken.jwt.audience");
        }
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("启用 JWT AccessToken 时必须配置非空的 imaping.token.accessToken.jwt.secret");
        }
        if (jwtProperties.usesDefaultSecret()) {
            throw new IllegalStateException("启用 JWT AccessToken 时禁止使用默认开发密钥,请显式配置 imaping.token.accessToken.jwt.secret");
        }
        if (jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT HS256 签名密钥长度至少需要 32 字节");
        }
    }
}

