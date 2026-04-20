package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Token Cookie 安全配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenCookieProperties {

    /**
     * 是否开启 HttpOnly。
     */
    private boolean httpOnly = true;

    /**
     * 是否仅允许 HTTPS 传输。
     */
    private boolean secure = false;

    /**
     * SameSite 策略。
     */
    private String sameSite = "Lax";

    /**
     * Cookie 生效域名,为空时由浏览器按当前域处理。
     */
    private String domain;

    /**
     * Cookie 生效路径。
     */
    private String path = "/";
}
