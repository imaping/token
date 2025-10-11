package com.imaping.token.api.generator;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Base64RandomStringGenerator extends AbstractRandomStringGenerator {

    public Base64RandomStringGenerator(final long defaultLength) {
        super(defaultLength);
    }

    /**
     * Converts byte[] to String by Base64 encoding.
     *
     * @param random raw bytes
     * @return a converted String
     */
    @Override
    protected String convertBytesToString(final byte[] random) {
        return EncodingUtils.encodeUrlSafeBase64(random);
    }

}
