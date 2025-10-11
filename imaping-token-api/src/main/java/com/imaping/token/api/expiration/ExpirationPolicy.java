package com.imaping.token.api.expiration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.model.Token;

import java.io.Serializable;
import java.time.Clock;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface ExpirationPolicy extends Serializable {

    /**
     * Method to determine if a {@link Token} has expired or not, based on the policy.
     *
     * @param tokenState The snapshot of the current token state
     * @return true if the token is expired, false otherwise.
     */
    boolean isExpired(Token tokenState);

    /**
     * Method to determine the actual TTL of a {@link Token}, based on the policy.
     *
     * @param tokenState The snapshot of the current token state
     * @return The time to live in seconds. A zero value indicates the time duration is not supported or is inactive.
     */
    default Long getTimeToLive(final Token tokenState) {
        return getTimeToLive();
    }

    /**
     * Describes the time duration where this policy should consider the item alive.
     * Once this time passes, the item is considered expired and dead.
     *
     * @return time to live in seconds. A zero value indicates the time duration is not supported or is inactive.
     */
    Long getTimeToLive();

    /**
     * Describes the idle time duration for the item.
     *
     * @return idle time in seconds. A zero value indicates the time duration is not supported or is inactive. Unit of measure is defined by the implementation.
     */
    Long getTimeToIdle();

    /**
     * Gets name of this expiration policy.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets clock of this expiration policy.
     *
     * @return the clock
     */
    @JsonIgnore
    Clock getClock();
}
