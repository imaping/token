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
 * 固定时间token
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
public class HardTimeoutExpirationPolicy extends AbstractTokenExpirationPolicy {

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
