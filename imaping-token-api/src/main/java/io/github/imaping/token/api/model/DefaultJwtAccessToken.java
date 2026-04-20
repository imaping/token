package io.github.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.ExpirationPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JWT 模式下的访问令牌实体。
 *
 * <p>服务端保留注册表记录用于注销、会话管理和 refresh token 续签,
 * 客户端暴露的访问令牌字符串由 JWT 编解码器负责生成。</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Setter
@NoArgsConstructor
@Getter
public class DefaultJwtAccessToken extends AbstractToken implements TimeoutAccessToken {

    private static final long serialVersionUID = -4886739319804366306L;

    public DefaultJwtAccessToken(final String id, final ExpirationPolicy expirationPolicy, final Authentication<?> authentication) {
        super(id, expirationPolicy, authentication);
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
