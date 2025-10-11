package com.imaping.token.api.expiration.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.model.Token;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Duration;

@FunctionalInterface
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface ExpirationPolicyBuilder<T extends Token> extends Serializable {

    /**
     * Method build token expiration policy.
     *
     * @return - the policy
     */
    ExpirationPolicy buildTokenExpirationPolicy();

    default Duration newDuration(final String value) {
        if ("0".equalsIgnoreCase(value) || "NEVER".equalsIgnoreCase(value) || !StringUtils.hasText(value)) {
            return Duration.ZERO;
        }
        if ("-1".equalsIgnoreCase(value) || !StringUtils.hasText(value) || "INFINITE".equalsIgnoreCase(value)) {
            return Duration.ofDays(Integer.MAX_VALUE);
        }
        if (NumberUtils.isCreatable(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        return Duration.parse(value);
    }
}
