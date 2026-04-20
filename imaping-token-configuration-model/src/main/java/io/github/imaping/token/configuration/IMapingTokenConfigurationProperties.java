package io.github.imaping.token.configuration;

import io.github.imaping.token.configuration.model.token.TokenConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token 组件专用配置属性。
 *
 * <p>仅绑定 {@code imaping.token.*} 配置项,供 token 相关模块直接使用,
 * 避免无关配置耦合到核心认证能力中。</p>
 */
@ConfigurationProperties(prefix = "imaping.token")
public class IMapingTokenConfigurationProperties extends TokenConfigurationProperties {
}
