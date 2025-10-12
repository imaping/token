package com.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的固定时间访问令牌实现.
 *
 * <p><b>序列化要求:</b> 继承自 AbstractToken,通过 Redis 持久化存储,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see HardTimeoutToken
 * @see AbstractToken
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Slf4j
@Setter
@NoArgsConstructor
@Getter
public class DefaultHardTimeoutToken extends AbstractToken implements HardTimeoutToken {

    /**
     * 保留 serialVersionUID 以确保 Redis 序列化兼容性.
     */
    private static final long serialVersionUID = 6382497067285418560L;

    private String description;

    private String code;

    public DefaultHardTimeoutToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication, String code, String description) {
        super(id, expirationPolicy, authentication);
        this.code = code;
        this.description = description;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
