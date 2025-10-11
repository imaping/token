package com.imaping.token.api.expiration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Clock;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public abstract class AbstractTokenExpirationPolicy implements ExpirationPolicy {

    private static final long serialVersionUID = 3122233439159209196L;
    private String name;

    private Clock clock = Clock.systemUTC();

    protected AbstractTokenExpirationPolicy() {
        this.name = this.getClass().getSimpleName() + '-' + UUID.randomUUID();
    }
}
