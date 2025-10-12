package com.imaping.token.api.expiration.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.TimeoutExpirationPolicy;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 自动续期过期策略构建器 - 创建 TimeoutExpirationPolicy 实例.
 *
 * <p><b>序列化要求:</b> Builder 对象可能被序列化以支持配置持久化,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see TimeoutExpirationPolicy
 * @see ExpirationPolicyBuilder
 */
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@Getter
public class TimeoutExpirationPolicyBuilder implements ExpirationPolicyBuilder<TimeoutAccessToken> {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     */
    private static final long serialVersionUID = 4176329929374375103L;

    private final IMapingConfigurationProperties properties;

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy() {
        return new TimeoutExpirationPolicy(newDuration(properties.getToken().getAccessToken().getTimeToKillInSeconds()).getSeconds());
    }
}
