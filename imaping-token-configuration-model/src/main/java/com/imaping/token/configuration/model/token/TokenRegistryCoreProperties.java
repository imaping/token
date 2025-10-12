package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Token 注册表核心配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class TokenRegistryCoreProperties {

    /**
     * When set to true, registry operations will begin to support
     * distributed locking for token operations. If the registry
     * itself supports distributed locking, such as JDBC or Redis,
     * then the lock implementation will defer to that option. Otherwise
     * the default locking solution will be specific to a CAS server node,
     * until replaced with a lock implementation or different locking option
     * separate from the registry technology itself.
     */
    private boolean enableLocking = true;
}
