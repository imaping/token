package com.imaping.token.api.expiration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.model.Token;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 固定时间过期策略 - Token 从创建时刻起固定时间后过期.
 *
 * <p>Token 过期时间固定,不会因使用而延长,适用于短期凭证场景.</p>
 *
 * <p><b>序列化要求:</b> 作为 Token 的一部分存储在 Redis 中,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see AbstractTokenExpirationPolicy
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
public class HardTimeoutExpirationPolicy extends AbstractTokenExpirationPolicy {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     */
    private static final long serialVersionUID = 5556499114310104800L;

    private long timeToKillInSeconds;

    @JsonCreator
    public HardTimeoutExpirationPolicy(@JsonProperty("timeToLive") final long timeToKillInSeconds) {
        this.timeToKillInSeconds = timeToKillInSeconds;
    }

    @Override
    public boolean isExpired(Token tokenState) {
        if (tokenState == null) {
            return true;
        }
        ZonedDateTime expiringTime = tokenState.getCreationTime().plus(this.timeToKillInSeconds, ChronoUnit.SECONDS);
        return expiringTime.isBefore(ZonedDateTime.now(getClock()));
    }

    @Override
    public Long getTimeToLive() {
        return timeToKillInSeconds;
    }

    @JsonIgnore
    @Override
    public Long getTimeToIdle() {
        return 0L;
    }
}
