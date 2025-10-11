package com.imaping.token.configuration.model.user;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class PasswordProperties implements Serializable {
    private static final long serialVersionUID = 1212095910003142762L;

    private String defaultPassword = "psh@123";
}
