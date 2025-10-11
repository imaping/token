package com.imaping.token.api.common;

public interface Cleanable {
    /**
     * Purges records meeting arbitrary criteria defined by implementers.
     */
    void clean();
}
