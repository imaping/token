package com.imaping.token.api.registry;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoOpTokenRegistryCleaner implements TokenRegistryCleaner {

    private static TokenRegistryCleaner INSTANCE;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static TokenRegistryCleaner getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpTokenRegistryCleaner();
        }
        return INSTANCE;
    }
}
