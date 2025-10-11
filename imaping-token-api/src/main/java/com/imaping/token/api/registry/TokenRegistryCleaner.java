package com.imaping.token.api.registry;

import com.imaping.token.api.model.Token;

public interface TokenRegistryCleaner {
    
    /**
     * Clean the token registry by collecting
     * tokens in the storage unit that may be expired.
     *
     * @return the int
     */
    default int clean() {
        return 0;
    }

    /**
     * Cleans up after an already-expired token, by running the necessary processes
     * such as logout notifications and more.
     *
     * @param token the token
     * @return the number of tokens that were cleaned up during the process.
     */
    default int cleanToken(final Token token) {
        return 0;
    }
}
