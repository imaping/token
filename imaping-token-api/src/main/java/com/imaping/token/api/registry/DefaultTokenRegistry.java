package com.imaping.token.api.registry;

import com.imaping.token.api.model.Token;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DefaultTokenRegistry extends AbstractMapBasedTokenRegistry {

    /**
     * A map to contain the tokens.
     */
    private final Map<String, Token> mapInstance;


    public DefaultTokenRegistry() {
        this.mapInstance = new ConcurrentHashMap<>();
    }

    public DefaultTokenRegistry(final Map<String, Token> storageMap) {
        this.mapInstance = storageMap;
    }
}
