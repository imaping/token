package com.imaping.token.core;

import com.imaping.token.core.model.DefaultSecurityUserInfoContext;
import com.imaping.token.core.model.DefaultUserInfoContext;
import com.imaping.token.core.model.SecurityUserInfoContext;
import com.imaping.token.core.model.UserInfoContext;
import com.imaping.token.core.util.SecurityContextUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author miaoj
 */
@Configuration
@ComponentScan(basePackages = {
        "com.imaping.token.core.model"
})
public class TokenCoreAutoConfig {

    @Bean(name = UserInfoContext.BEAN_NAME)
    @ConditionalOnMissingBean(name = UserInfoContext.BEAN_NAME)
    public UserInfoContext userInfoContext() {
        return new DefaultUserInfoContext();
    }

    @Bean(name = SecurityUserInfoContext.BEAN_NAME)
    @ConditionalOnMissingBean(name = SecurityUserInfoContext.BEAN_NAME)
    public SecurityUserInfoContext securityUserInfoContext() {
        return new DefaultSecurityUserInfoContext();
    }

    @Bean
    public SecurityContextUtil securityContextUtil(UserInfoContext userInfoContext) {
        final SecurityContextUtil securityContextUtil = new SecurityContextUtil();
        securityContextUtil.setUserInfoContext(userInfoContext);
        return securityContextUtil;
    }
}
