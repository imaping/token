package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class AccessTokenProperties implements Serializable {
    private static final long serialVersionUID = 7783230106761680880L;


    private String timeToKillInSeconds = "PT2H";

    private boolean createAsJwt;
}
