package com.imaping.token.configuration.model.cas;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * CAS 配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class CasProperties {

    private String prefix;

    private String validateUrl;

    public String getValidateUrl() {
        Assert.notNull(prefix, "cas prefix is not can be null");
        if (!StringUtils.hasText(validateUrl)) {
            return prefix + "/p3/serviceValidate";
        }
        return prefix;
    }
}
