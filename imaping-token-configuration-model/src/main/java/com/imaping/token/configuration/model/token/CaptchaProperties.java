package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author miaoj
 */
@Getter
@Setter
@Accessors(chain = true)
public class CaptchaProperties implements Serializable {
    private static final long serialVersionUID = 6692492464139270886L;


    private boolean enabled = true;
}
