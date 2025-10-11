package com.imaping.token.api.config;

import com.imaping.token.api.boot.ConditionalOnMatchingHostname;
import com.imaping.token.api.common.BeanCondition;
import com.imaping.token.api.common.BeanSupplier;
import com.imaping.token.api.common.Cleanable;
import com.imaping.token.api.common.FunctionUtils;
import com.imaping.token.api.lock.LockRepository;
import com.imaping.token.configuration.DubheConfigurationProperties;
import com.imaping.token.api.registry.DefaultTokenRegistryCleaner;
import com.imaping.token.api.registry.NoOpTokenRegistryCleaner;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.api.registry.TokenRegistryCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@EnableAsync
//@EnableTransactionManagement
@Slf4j
public class TokenSchedulingConfiguration {
    @ConditionalOnMissingBean(name = "tokenRegistryCleaner")
    @Bean
    public TokenRegistryCleaner tokenRegistryCleaner(
            final DubheConfigurationProperties properties,
            @Qualifier(LockRepository.BEAN_NAME) final LockRepository lockRepository,
            @Qualifier(TokenRegistry.BEAN_NAME) final TokenRegistry tokenRegistry) {
        val isCleanerEnabled = properties.getToken().getRegistry().getCleaner().getSchedule().isEnabled();
        if (isCleanerEnabled) {
            log.debug("Token registry cleaner is enabled.");
            return new DefaultTokenRegistryCleaner(lockRepository, tokenRegistry);
        }
        log.debug("Token registry cleaner is not enabled. "
                + "Expired tokens are not forcefully cleaned by CAS. It is up to the token registry itself to "
                + "clean up tokens based on its own expiration and eviction policies.");
        return NoOpTokenRegistryCleaner.getInstance();
    }

    @ConditionalOnMissingBean(name = "tokenRegistryCleanerScheduler")
    @ConditionalOnMatchingHostname(name = "dubhe.token.registry.cleaner.schedule.enabled-on-host")
    @Bean
    public Cleanable tokenRegistryCleanerScheduler(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier("tokenRegistryCleaner") final TokenRegistryCleaner tokenRegistryCleaner) throws Exception {
        return BeanSupplier.of(Cleanable.class)
                .when(BeanCondition.on("dubhe.token.registry.cleaner.schedule.enabled").isTrue()
                        .evenIfMissing().given(applicationContext.getEnvironment()))
                .supply(() -> new TokenRegistryCleanerScheduler(tokenRegistryCleaner))
                .otherwiseProxy()
                .get();
    }

    @RequiredArgsConstructor
    public static class TokenRegistryCleanerScheduler implements Cleanable {
        private final TokenRegistryCleaner tokenRegistryCleaner;

        @Scheduled(initialDelayString = "${dubhe.token.registry.cleaner.schedule.start-delay:PT30S}",
                fixedDelayString = "${dubhe.token.registry.cleaner.schedule.repeat-interval:PT120S}")
        @Override
        public void clean() {
            FunctionUtils.doAndHandle(unused -> tokenRegistryCleaner.clean());
        }
    }
}
