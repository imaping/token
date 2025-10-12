package com.imaping.token.api.expiration.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import com.imaping.token.api.model.HardTimeoutToken;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 固定时间过期策略构建器默认实现 - 创建 HardTimeoutExpirationPolicy 实例.
 *
 * <p><b>序列化要求:</b> Builder 对象可能被序列化以支持配置持久化,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see HardTimeoutExpirationPolicy
 * @see HardTimeoutExpirationPolicyBuilder
 */
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@Getter
public class HardTimeoutExpirationPolicyDefaultBuilder<T extends HardTimeoutToken> implements HardTimeoutExpirationPolicyBuilder<T> {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     */
    private static final long serialVersionUID = -4105381841515569079L;

    private final IMapingConfigurationProperties properties;

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy() {
        return buildTokenExpirationPolicy(newDuration(properties.getToken().getAccessToken().getTimeToKillInSeconds()).getSeconds());
    }

    @Override
    public ExpirationPolicy buildTokenExpirationPolicy(long timeToKillInSeconds) {
        return new HardTimeoutExpirationPolicy(timeToKillInSeconds);
    }
}
