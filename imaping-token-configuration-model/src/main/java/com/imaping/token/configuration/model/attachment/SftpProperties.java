package com.imaping.token.configuration.model.attachment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class SftpProperties implements Serializable {

    private static final long serialVersionUID = -2276512851177657849L;
    private String host;

    private int port;

    private String username;

    private String password;

    private String root;
}
