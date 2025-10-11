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
 * 自动续期，活动时间到期内过期
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
public class TimeoutExpirationPolicy extends AbstractTokenExpirationPolicy {

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
