package com.imaping.token.resource.client.aware;

import com.imaping.token.core.model.UserInfo;
import com.imaping.token.core.model.UserInfoContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * jpa自动注入当前用户信息
 *
 * @author miaoj
 */
@Component
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.config.JpaRepositoriesRegistrar")
public class CurrentUserAutoAware implements AuditorAware<String> {

    private final UserInfoContext userInfoContext;

    public CurrentUserAutoAware(UserInfoContext userInfoContext) {
        this.userInfoContext = userInfoContext;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        final UserInfo<?> currentUserInfo = userInfoContext.getCurrentUserInfo();
        if (currentUserInfo.isAuthenticated()) {
            // 将泛型 ID 安全转换为字符串,用于审计字段
            return Optional.of(String.valueOf(currentUserInfo.getId()));
        }
        return Optional.empty();
    }
}
