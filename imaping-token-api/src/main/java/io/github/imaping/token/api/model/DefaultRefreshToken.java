package io.github.imaping.token.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.ExpirationPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 默认的 refresh token 实现。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Setter
@NoArgsConstructor
@Getter
public class DefaultRefreshToken extends AbstractToken implements RefreshToken {

    private static final long serialVersionUID = 8822269190667448514L;

    private String accessTokenId;

    public DefaultRefreshToken(final String id,
                               final ExpirationPolicy expirationPolicy,
                               final Authentication<?> authentication,
                               final String accessTokenId) {
        super(id, expirationPolicy, authentication);
        this.accessTokenId = accessTokenId;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
