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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 自动续期过期策略 - 活动时间到期内过期.
 *
 * <p>Token 在每次使用后重置过期时间,适用于需要自动续期的场景.</p>
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
public class TimeoutExpirationPolicy extends AbstractTokenExpirationPolicy {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     */
    private static final long serialVersionUID = -3427615304636639301L;

    private long timeToKillInSeconds;

    @JsonCreator
    public TimeoutExpirationPolicy(@JsonProperty("timeToIdle") final long timeToKillInSeconds) {
        this.timeToKillInSeconds = timeToKillInSeconds;
    }

    @Override
    public boolean isExpired(Token tokenState) {
        ZonedDateTime currentSystemTime = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expirationTimeToKill = tokenState.getLastTimeUsed().plus(this.timeToKillInSeconds, ChronoUnit.SECONDS);
        if (currentSystemTime.isAfter(expirationTimeToKill)) {
            log.debug(" token is expired because the current time [{}] is after [{}]", currentSystemTime, expirationTimeToKill);
            return true;
        }
        return false;
    }

    @JsonIgnore
    @Override
    public Long getTimeToLive() {
        return 0L;
    }

    @Override
    public Long getTimeToIdle() {
        return timeToKillInSeconds;
    }
}
