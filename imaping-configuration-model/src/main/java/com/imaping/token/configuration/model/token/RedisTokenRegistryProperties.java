package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class RedisTokenRegistryProperties implements Serializable {
    private static final long serialVersionUID = -8170973087991927384L;

    private boolean enabled = true;
}
