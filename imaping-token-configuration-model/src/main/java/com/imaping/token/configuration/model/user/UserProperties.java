package com.imaping.token.configuration.model.user;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class UserProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 7442143459197056640L;


    @NestedConfigurationProperty
    private PasswordProperties password = new PasswordProperties();
}
