package com.imaping.token.api.generator;

import org.apache.commons.codec.binary.Base64;

public class EncodingUtils {
    public static String encodeUrlSafeBase64(final byte[] data) {
        return Base64.encodeBase64URLSafeString(data);
    }
}
