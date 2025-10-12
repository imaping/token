package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class CloudProperties implements Serializable {
    private static final long serialVersionUID = 5032805801421565272L;

    private boolean enabled = false;
}
