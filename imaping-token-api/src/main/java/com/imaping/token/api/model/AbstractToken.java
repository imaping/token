package com.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.authentication.AuthenticationAwareToken;
import com.imaping.token.api.expiration.ExpirationPolicy;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.time.ZonedDateTime;

/**
 * Token 抽象基类.
 *
 * <p><b>序列化要求:</b> 此类及其子类通过 Redis 持久化存储,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @see Token
 * @see AuthenticationAwareToken
 * @since 0.0.1
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Setter
@Slf4j
public abstract class AbstractToken implements Token, AuthenticationAwareToken {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     * Token 对象会存储在 Redis 中,需要跨版本兼容性.
     */
    @Serial
    private static final long serialVersionUID = -4232605651875239941L;

    @Getter
    private String id;

    @Getter
    private ExpirationPolicy expirationPolicy;

    @Getter
    private ZonedDateTime lastTimeUsed;

    /**
     * The previous last time this token was used.
     */
    @Getter
    private ZonedDateTime previousTimeUsed;

    /**
     * The time the token was created.
     */
    @Getter
    private ZonedDateTime creationTime;

    /**
     * The number of times this was used.
     */
    @Getter
    private int countOfUses;

    /**
     * Flag to enforce manual expiration.
     */
    private Boolean expired = Boolean.FALSE;

    @Getter
    private Authentication authentication;

    protected AbstractToken(final String id, final ExpirationPolicy expirationPolicy, Authentication authentication) {
        this.id = id;
        this.creationTime = ZonedDateTime.now(expirationPolicy.getClock());
        this.lastTimeUsed = this.creationTime;
        this.expirationPolicy = expirationPolicy;
        this.authentication = authentication;
        authentication.getPrincipal().getUserInfo().setAccessToken(id);
    }

    @Override
    public void update() {
        log.trace("Before updating token [{}]\n\tPrevious time used: [{}]\n\tLast time used: [{}]\n\tUsage count: [{}]",
                getId(), this.previousTimeUsed, this.lastTimeUsed, this.countOfUses);

        this.previousTimeUsed = ZonedDateTime.from(this.lastTimeUsed);
        this.lastTimeUsed = ZonedDateTime.now(this.expirationPolicy.getClock());
        this.countOfUses++;

        log.trace("After updating token [{}]\n\tPrevious time used: [{}]\n\tLast time used: [{}]\n\tUsage count: [{}]",
                getId(), this.previousTimeUsed, this.lastTimeUsed, this.countOfUses);
    }

    @Override
    public boolean isExpired() {
        return this.expirationPolicy.isExpired(this) || isExpiredInternal();
    }

    @Override
    public int compareTo(final Token o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public void markTokenExpired() {
        this.expired = Boolean.TRUE;
    }

    @JsonIgnore
    protected boolean isExpiredInternal() {
        return this.expired;
    }
}
