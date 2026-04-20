package io.github.imaping.token.api.session;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Token 会话快照。
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "tokenId")
public class TokenSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 8960346950901836187L;

    /**
     * Token 标识。
     */
    private final String tokenId;

    /**
     * 当前会话所属用户。
     */
    private final String principalId;

    /**
     * Token 类型名称。
     */
    private final String tokenType;

    /**
     * 创建时间。
     */
    private final ZonedDateTime creationTime;

    /**
     * 最后使用时间。
     */
    private final ZonedDateTime lastTimeUsed;

    /**
     * 上一次使用时间。
     */
    private final ZonedDateTime previousTimeUsed;

    /**
     * 使用次数。
     */
    private final int countOfUses;

    /**
     * 是否已过期。
     */
    private final boolean expired;

    /**
     * 是否为当前请求正在使用的会话。
     */
    private final boolean current;
}
