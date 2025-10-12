package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 验证码配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author miaoj
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class CaptchaProperties {

    private boolean enabled = true;
}
