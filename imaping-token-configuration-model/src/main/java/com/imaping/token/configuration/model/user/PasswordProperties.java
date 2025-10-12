package com.imaping.token.configuration.model.user;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 密码配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class PasswordProperties {

    private String defaultPassword = "psh@123";
}
