package io.github.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证失败跳转配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenFailureRedirectProperties {

    /**
     * 是否启用基于请求参数的失败跳转。
     * 默认关闭,避免开放重定向风险。
     */
    private boolean enabled = false;

    /**
     * 是否允许绝对 URL。
     */
    private boolean allowAbsoluteUrls = false;

    /**
     * 允许跳转的主机名白名单。
     * 仅在 allowAbsoluteUrls=true 时生效。
     */
    private List<String> allowedHosts = new ArrayList<>();
}
