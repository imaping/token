package com.imaping.token.api.factory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.imaping.token.api.model.Token;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class DefaultTokenFactory implements TokenFactory {

    private final Map<String, TokenFactory> factoryMap = new HashMap<>(0);

    @Override
    public TokenFactory get(final Class<? extends Token> clazz) {
        return this.factoryMap.get(clazz.getCanonicalName());
    }

    /**
     * Add token factory.
     *
     * @param tokenClass the token class
     * @param factory     the factory
     * @return the default token factory
     */
    @CanIgnoreReturnValue
    public DefaultTokenFactory addTokenFactory(final @NonNull Class<? extends Token> tokenClass,
                                               final @NonNull TokenFactory factory) {
        this.factoryMap.put(tokenClass.getCanonicalName(), factory);
        return this;
    }

    @Override
    public Class<? extends Token> getTokenType() {
        return Token.class;
    }
}
