package com.imaping.token.api.factory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.imaping.token.api.model.Token;

@FunctionalInterface
public interface TokenFactory {

    /**
     * Default implementation bean name.
     */
    String BEAN_NAME = "defaultTokenFactory";

    /**
     * Gets name.
     *
     * @return the name
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Get token factory.
     *
     * @param clazz the clazz
     * @return token factory object
     */
    @CanIgnoreReturnValue
    default TokenFactory get(Class<? extends Token> clazz) {
        return this;
    }

    /**
     * Gets token type.
     *
     * @return the token type
     */
    Class<? extends Token> getTokenType();
}