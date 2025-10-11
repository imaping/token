package com.imaping.token.api.registry;

import com.imaping.token.api.authentication.AuthenticationAwareToken;
import com.imaping.token.api.model.DefaultTimeoutAccessToken;
import com.imaping.token.api.model.Token;
import org.jooq.lambda.Unchecked;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface TokenRegistry {

    /**
     * Default bean name.
     */
    String BEAN_NAME = "tokenRegistry";

    /**
     * Add a token to the registry. Token storage is based on the token id.
     *
     * @param token The token we wish to add to the cache.
     * @throws Exception the exception
     */
    void addToken(Token token) throws Exception;

    /**
     * Save.
     *
     * @param toSave the to save
     * @throws Exception the exception
     */
    default void addToken(final Stream<? extends Token> toSave) throws Exception {
        toSave.forEach(Unchecked.consumer(this::addToken));
    }

    /**
     * Retrieve a token from the registry. If the token retrieved does not
     * match the expected class, an InvalidTokenException is thrown.
     *
     * @param <T>     the generic token type to return that extends {@link Token}
     * @param tokenId the id of the token we wish to retrieve.
     * @param clazz   The expected class of the token we wish to retrieve.
     * @return the requested token.
     */
    <T extends Token> T getToken(String tokenId, Class<T> clazz);

    /**
     * Retrieve a token from the registry.
     *
     * @param tokenId the id of the token we wish to retrieve
     * @return the requested token.
     */
    Token getToken(String tokenId);

    /**
     * Gets token from registry using a predicate.
     *
     * @param tokenId   the token id
     * @param predicate the predicate that tests the token
     * @return the token
     */
    Token getToken(String tokenId, Predicate<Token> predicate);

    /**
     * Remove a specific token from the registry.
     * If token to delete is TGT then related service tokens are removed as well.
     *
     * @param tokenId The id of the token to delete.
     * @return the number of tokens deleted including children.
     * @throws Exception the exception
     */
    int deleteToken(String tokenId) throws Exception;

    /**
     * Remove a specific token from the registry.
     * If token to delete is TGT then related service tokens, etc are removed as well.
     *
     * @param tokenId The id of the token to delete.
     * @return the number of tokens deleted including children.
     * @throws Exception the exception
     */
    int deleteToken(Token tokenId) throws Exception;

    /**
     * Delete all tokens from the registry.
     *
     * @return the number of tokens deleted.
     */
    long deleteAll();

    /**
     * Retrieve all tokens from the registry.
     *
     * @return collection of tokens currently stored in the registry. Tokens might or might not be valid i.e. expired.
     */
    Collection<? extends Token> getTokens();

    /**
     * Gets tokens as a stream having applied a predicate.
     * <p>
     * The returning stream may be bound to an IO channel (such as database connection),
     * so it should be properly closed after usage.
     *
     * @param predicate the predicate
     * @return the tokens
     */
    default Stream<? extends Token> getTokens(final Predicate<Token> predicate) {
        return stream().filter(predicate);
    }

    /**
     * Update the received token.
     *
     * @param token the token
     * @return the updated token
     * @throws Exception the exception
     */
    Token updateToken(Token token) throws Exception;

    /**
     * Computes the number of SSO sessions stored in the token registry.
     *
     * @return Number of token-granting tokens in the registry at time of invocation or {@link Integer#MIN_VALUE} if unknown.
     */
    long sessionCount();

    /**
     * Gets tokens stream.
     * <p>
     * The returning stream may be bound to an IO channel (such as database connection),
     * so it should be properly closed after usage.
     *
     * @return the tokens stream
     */
    default Stream<? extends Token> stream() {
        return getTokens().stream();
    }

    /**
     * Count the number of single sign-on sessions
     * that are recorded in the token registry for
     * the given user name.
     *
     * @param principalId the principal id
     * @return the count
     */
    long countSessionsFor(String principalId);

    /**
     * Gets sessions for principal.
     *
     * @param principalId the principal id
     * @return the sessions for
     */
    default Stream<? extends Token> getSessionsFor(final String principalId) {
        return getTokens(token -> token instanceof DefaultTimeoutAccessToken
                && !token.isExpired()
                && ((AuthenticationAwareToken) token).getAuthentication().getPrincipal().getId().equals(principalId));
    }
}
