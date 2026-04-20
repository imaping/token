package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * JWT 访问令牌配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenJwtProperties {

    /**
     * 默认开发密钥。
     */
    public static final String DEFAULT_SECRET = "ChangeThisJwtSecretKeyBeforeProductionUse1234567890";

    /**
     * JWT 签发者。
     */
    private String issuer = "imaping-token";

    /**
     * JWT 受众。
     */
    private String audience = "imaping-token-resource";

    /**
     * HS256 签名密钥。
     * 生产环境必须覆盖默认值。
     */
    private String secret = DEFAULT_SECRET;

    /**
     * 时钟偏移容忍秒数。
     */
    private long allowedClockSkewSeconds = 30L;

    /**
     * 是否仍在使用默认开发密钥。
     */
    public boolean usesDefaultSecret() {
        return DEFAULT_SECRET.equals(secret);
    }
}
