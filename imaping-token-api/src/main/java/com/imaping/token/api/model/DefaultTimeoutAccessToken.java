package com.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的自动续期访问令牌实现.
 *
 * <p><b>序列化要求:</b> 继承自 AbstractToken,通过 Redis 持久化存储,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see TimeoutAccessToken
 * @see AbstractToken
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@Setter
@NoArgsConstructor
@Getter
public class DefaultTimeoutAccessToken extends AbstractToken implements TimeoutAccessToken {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     */
    private static final long serialVersionUID = 5024818450360479885L;

    public DefaultTimeoutAccessToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication) {
        super(id, expirationPolicy, authentication);
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
