package com.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;

import java.io.Serializable;
import java.time.ZonedDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Token extends Serializable, Comparable<Token>{

    /**
     * Method to retrieve the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Method to return the time the token was created.
     *
     * @return the time the token was created.
     */
    ZonedDateTime getCreationTime();

    /**
     * Gets count of uses.
     *
     * @return the number of times this token was used.
     */
    int getCountOfUses();

    /**
     * Gets prefix.
     *
     * @return the prefix
     */
    String getPrefix();

    /**
     * Determines if the token is expired. Most common implementations might
     * collaborate with <i>ExpirationPolicy</i> strategy.
     *
     * @return true, if the token is expired
     * @see ExpirationPolicy
     */
    boolean isExpired();

    /**
     * Get expiration policy associated with token.
     *
     * @return the expiration policy
     */
    ExpirationPolicy getExpirationPolicy();

    /**
     * Mark a token as expired.
     */
    void markTokenExpired();

    /**
     * Returns the last time the token was used.
     *
     * @return the last time the token was used.
     */
    ZonedDateTime getLastTimeUsed();

    /**
     * Get the second to last time used.
     *
     * @return the previous time used.
     */
    ZonedDateTime getPreviousTimeUsed();

    /**
     * Records the <i>previous</i> last time this token was used as well as
     * the last usage time. The token usage count is also incremented.
     * <p>tokens themselves are solely responsible to maintain their state. The
     * determination of token usage is left up to the implementation and
     * the specific token type.
     *
     * @see ExpirationPolicy
     * @since 5.0.0
     */
    void update();
}
