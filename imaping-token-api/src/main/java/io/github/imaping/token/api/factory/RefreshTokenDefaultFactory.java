package io.github.imaping.token.api.factory;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import io.github.imaping.token.api.generator.UniqueTokenIdGenerator;
import io.github.imaping.token.api.model.DefaultRefreshToken;
import io.github.imaping.token.api.model.RefreshToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 默认的 refresh token 工厂。
 */
@RequiredArgsConstructor
public class RefreshTokenDefaultFactory implements RefreshTokenFactory {

    private final UniqueTokenIdGenerator idGenerator;
    private final IMapingTokenConfigurationProperties properties;

    @Override
    public RefreshToken create(final Authentication<?> authentication, final String accessTokenId) {
        Duration ttl = parseDuration(properties.getRefreshToken().getTimeToKillInSeconds());
        return new DefaultRefreshToken(
                idGenerator.getNewTokenId(RefreshToken.PREFIX),
                new HardTimeoutExpirationPolicy(ttl.getSeconds()),
                authentication,
                accessTokenId
        );
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return RefreshToken.class;
    }

    private Duration parseDuration(final String value) {
        if ("0".equalsIgnoreCase(value) || "NEVER".equalsIgnoreCase(value) || value == null || value.isBlank()) {
            return Duration.ZERO;
        }
        if ("-1".equalsIgnoreCase(value) || "INFINITE".equalsIgnoreCase(value)) {
            return Duration.ofDays(Integer.MAX_VALUE);
        }
        if (value.chars().allMatch(Character::isDigit)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        return Duration.parse(value);
    }
}
