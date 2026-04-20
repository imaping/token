package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Token 传输介质配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenTransportProperties {

    /**
     * 是否允许通过 Authorization: Bearer 传递 token。
     */
    private boolean allowAuthorizationHeader = true;

    /**
     * 是否允许通过与 accessTokenName 同名的请求头传递 token。
     */
    private boolean allowNamedHeader = true;

    /**
     * 是否允许通过 Cookie 传递 token。
     */
    private boolean allowCookie = true;

    /**
     * 是否允许通过 URL/Form 参数传递 token。
     * 默认关闭,避免 token 暴露到日志、浏览器历史和代理链路中。
     */
    private boolean allowRequestParameter = false;
}
