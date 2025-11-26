package com.imaping.token.api.registry;

import com.imaping.token.api.authentication.AuthenticationAwareToken;
import com.imaping.token.api.exception.TokenAuthenticationException;
import com.imaping.token.api.exception.TokenError;
import com.imaping.token.api.model.DefaultTimeoutAccessToken;
import com.imaping.token.api.model.Token;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jooq.lambda.Unchecked;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTokenRegistry implements TokenRegistry {

    @Override
    public void addToken(final Token token) throws Exception {
        if (token != null && !token.isExpired()) {
            addTokentInternal(token);
        }
    }

    @Override
    public Token getToken(final String tokenId) {
        return getToken(tokenId, token -> {
            if (token == null || token.isExpired()) {
                log.debug("Token [{}] has expired and will be removed from the token registry", tokenId);
                deleteSingleToken(tokenId);
                return false;
            }
            return true;
        });
    }

    @Override
    public <T extends Token> T getToken(final String tokenId, final @NonNull Class<T> clazz) {
        Token token = getToken(tokenId);
        if (token == null) {
            log.debug("Token [{}] with type [{}] cannot be found", tokenId, clazz.getSimpleName());
            TokenError tokenError = new TokenError(TokenError.INVALID_TOKEN,
                    HttpStatus.UNAUTHORIZED,
                    String.format("Provided token [%s] is either not found in the token registry or has expired", tokenId));
            throw new TokenAuthenticationException(tokenError);
        }
        if (!clazz.isAssignableFrom(token.getClass())) {
            throw new ClassCastException("Token [" + token.getId() + " is of type "
                    + token.getClass() + " when we were expecting " + clazz);
        }
        return clazz.cast(token);
    }

    @Override
    public long sessionCount() {
        try (Stream<?> stream = stream().filter(DefaultTimeoutAccessToken.class::isInstance)) {
            return stream.count();
        } catch (final Exception t) {
            log.trace("sessionCount() operation is not implemented by the token registry instance [{}]. "
                    + "Message is: [{}] Returning unknown as [{}]", this.getClass().getName(), t.getMessage(), Long.MIN_VALUE);
            return Long.MIN_VALUE;
        }
    }

    @Override
    public long countSessionsFor(final String principalId) {
        val tokenPredicate = (Predicate<Token>) t -> {
            if (t instanceof DefaultTimeoutAccessToken) {
                val token = DefaultTimeoutAccessToken.class.cast(t);
                return token.getAuthentication().getPrincipal().getId().equalsIgnoreCase(principalId);
            }
            return false;
        };
        return getTokens(tokenPredicate).count();
    }


    @Override
    public int deleteToken(final String tokenId) throws Exception {
        if (!StringUtils.hasText(tokenId)) {
            log.trace("No token id is provided for deletion");
            return 0;
        }
        val token = getToken(tokenId);
        if (token == null) {
            log.debug("Token [{}] could not be fetched from the registry; it may have been expired and deleted.", tokenId);
            return 0;
        }
        return deleteToken(token);
    }

    @Override
    public int deleteToken(final Token token) throws Exception {
        AtomicLong count = new AtomicLong(0);
        log.debug("Removing token [{}] from the registry.", token);
        count.getAndAdd(deleteSingleToken(token.getId()));
        return count.intValue();
    }

    /**
     * Delete a single token instance from the store.
     *
     * @param tokenId the token id
     * @return true/false
     */
    public abstract long deleteSingleToken(String tokenId);

    /**
     * Add token internally by the
     * registry implementation.
     *
     * @param token the token
     * @throws Exception the exception
     */
    protected abstract void addTokentInternal(Token token) throws Exception;

    /**
     * Delete tokens.
     *
     * @param tokens the tokens
     * @return the total number of deleted tokens
     */
    protected int deleteTokens(final Set<String> tokens) {
        return deleteTokens(tokens.stream());
    }

    /**
     * Delete tokens.
     *
     * @param tokens the tokens
     * @return the total number of deleted tokens
     */
    protected int deleteTokens(final Stream<String> tokens) {
        return tokens.mapToInt(Unchecked.toIntFunction(this::deleteToken)).sum();
    }


    /**
     * Gets principal id from token.
     *
     * @param token the token
     * @return the principal id from
     */
    protected static String getPrincipalIdFrom(final Token token) {
        return token instanceof AuthenticationAwareToken
                ? Optional.ofNullable(((AuthenticationAwareToken) token).getAuthentication())
                .map(auth -> auth.getPrincipal().getId())
                .orElse("")
                : "";
    }
}
