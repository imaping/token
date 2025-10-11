package com.imaping.token.api.generator;

public interface LongNumericGenerator  extends NumericGenerator{
    /**
     * Get the next long in the sequence.
     *
     * @return the next long in the sequence.
     */
    long getNextLong();
}
