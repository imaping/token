package dubhe.token.test.config;

import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import com.imaping.token.resource.client.config.TokenSecurityConfig;
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

    public TestSecurityConfigurerAdapter(TokenRegistry tokenRegistry, @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory, IMapingConfigurationProperties properties) {
        super(tokenRegistry, tokenFactory, properties);
    }


//    @Override
//    protected String[] getPermitAntMatchers() {
//        return new String[]{"/cas/validate", "/login", "/logout"};
//    }

    @Override
    protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
        final Map<HttpMethod, String[]> matchers = super.getPermitAntMatchersWithMethod();
        matchers.put(HttpMethod.GET, new String[]{"/rest/business/userinfo", "/captcha", "/cas/*", "/rest/business/workflow"});
        matchers.put(HttpMethod.POST, new String[]{"/cas/*"});
        matchers.put(HttpMethod.OPTIONS, new String[]{"/**"});
        return matchers;
    }
}
