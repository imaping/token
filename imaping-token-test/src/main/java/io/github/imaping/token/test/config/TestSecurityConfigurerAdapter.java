package io.github.imaping.token.test.config;

import io.github.imaping.token.api.factory.TokenFactory;
import io.github.imaping.token.api.jwt.AccessTokenCodec;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.resource.client.config.TokenSecurityConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * 自定义安全配置
 * 主要自定义哪些接口需要登录才能访问
 * 哪些接口不需要登录就能访问
 * 若所有接口都要认证后才能访问，则不需要重写，使用默认配置即可
 */
@Configuration
public class TestSecurityConfigurerAdapter extends TokenSecurityConfig {

    public TestSecurityConfigurerAdapter(TokenRegistry tokenRegistry,
                                         @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory,
                                         @Qualifier(AccessTokenCodec.BEAN_NAME) AccessTokenCodec accessTokenCodec,
                                         IMapingTokenConfigurationProperties properties) {
        super(tokenRegistry, tokenFactory, accessTokenCodec, properties);
    }


    @Override
    protected String[] getPermitAntMatchers() {
        return new String[]{"/login", "/logout", "/refresh"};
    }

    @Override
    protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
        final Map<HttpMethod, String[]> matchers = super.getPermitAntMatchersWithMethod();
        matchers.put(HttpMethod.GET, new String[]{"/rest/business/userinfo", "/rest/business/workflow"});
        matchers.put(HttpMethod.OPTIONS, new String[]{"/**"});
        return matchers;
    }
}
