package io.github.imaping.token.api.jwt;

import io.github.imaping.token.api.authentication.AuthenticationAwareToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.configuration.IMapingTokenConfigurationProperties;
import io.github.imaping.token.configuration.model.token.TokenJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 默认访问令牌编解码器。
 */
@RequiredArgsConstructor
public class DefaultAccessTokenCodec implements AccessTokenCodec {

    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String TOKEN_USE_ACCESS = "access";
    private static final String ROLES_CLAIM = "roles";

    private final IMapingTokenConfigurationProperties properties;

    @Override
    public boolean isJwtEnabled() {
        return properties.getAccessToken().isCreateAsJwt();
    }

    @Override
    public String encode(final Token token) {
        if (!isJwtEnabled()) {
            return token.getId();
        }
        if (!(token instanceof AuthenticationAwareToken authenticationAwareToken)) {
            return token.getId();
        }
        final var principal = authenticationAwareToken.getAuthentication().getPrincipal();
        final ZonedDateTime expiresAt = token.getCreationTime()
                .plusSeconds(resolveJwtTtlSeconds(token));
        final TokenJwtProperties jwtProperties = properties.getAccessToken().getJwt();
        var builder = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(principal.getId())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(token.getCreationTime().toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .id(token.getId())
                .claim(TOKEN_USE_CLAIM, TOKEN_USE_ACCESS)
                .signWith(secretKey());
        Set<String> roles = principal.getUserInfo() != null ? principal.getUserInfo().getRoles() : null;
        if (roles != null && !roles.isEmpty()) {
            builder.claim(ROLES_CLAIM, List.copyOf(roles));
        }
        return builder.compact();
    }

    @Override
    public DecodedAccessToken decode(final String tokenValue) {
        if (!isJwtEnabled()) {
            return DecodedAccessToken.plain(tokenValue);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .clockSkewSeconds(properties.getAccessToken().getJwt().getAllowedClockSkewSeconds())
                    .build()
                    .parseSignedClaims(tokenValue)
                    .getPayload();
            if (!TOKEN_USE_ACCESS.equals(claims.get(TOKEN_USE_CLAIM, String.class))) {
                throw new JwtException("Unsupported token_use claim");
            }
            return DecodedAccessToken.jwt(tokenValue, claims.getId());
        } catch (JwtException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new JwtException("Failed to decode access token", ex);
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.getAccessToken().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private long resolveJwtTtlSeconds(final Token token) {
        Long ttl = token.getExpirationPolicy() != null ? token.getExpirationPolicy().getTimeToLive() : null;
        Long idle = token.getExpirationPolicy() != null ? token.getExpirationPolicy().getTimeToIdle() : null;
        if (ttl != null && ttl > 0) {
            return ttl;
        }
        if (idle != null && idle > 0) {
            return idle;
        }
        return 0L;
    }
}
