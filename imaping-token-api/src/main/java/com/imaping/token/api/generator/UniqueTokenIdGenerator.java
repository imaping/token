package com.imaping.token.api.generator;

@FunctionalInterface
public interface UniqueTokenIdGenerator {

    /**
     * Separator character that separates prefix from
     * the rest of the token id.
     */
    char SEPARATOR = '-';

    /**
     * Default token size 24 bytes raw, 32 bytes once encoded to base64.
     */
    int Token_SIZE = 24;

    /**
     * Return a new unique token id beginning with the prefix.
     *
     * @param prefix The prefix we want attached to the token.
     * @return the unique token id
     */
    String getNewTokenId(String prefix);
}
