package com.imaping.token.api.model;

/**
 * 固定访问时间token，过期自动失效/删除
 */
public interface HardTimeoutToken extends Token {
    String PREFIX = "ATT";

    String getDescription();

    String getCode();
}
