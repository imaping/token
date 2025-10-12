package com.imaping.token.api.expiration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Clock;
import java.util.UUID;

/**
 * Token 过期策略抽象基类.
 *
 * <p><b>序列化要求:</b> ExpirationPolicy 作为 Token 的一部分存储在 Redis 中,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see ExpirationPolicy
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public abstract class AbstractTokenExpirationPolicy implements ExpirationPolicy {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     * ExpirationPolicy 对象作为 Token 的一部分存储在 Redis 中.
     */
    private static final long serialVersionUID = 3122233439159209196L;
    private String name;

    private Clock clock = Clock.systemUTC();

    protected AbstractTokenExpirationPolicy() {
        this.name = this.getClass().getSimpleName() + '-' + UUID.randomUUID();
    }
}
