package com.imaping.token.configuration.model.cas;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class CasProperties implements Serializable {

    private static final long serialVersionUID = 3702974532966843701L;
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
