package com.imaping.token.configuration.model.attachment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class MinioProperties implements Serializable {

    private static final long serialVersionUID = 6031068177559847432L;
    private String host;

    private int port;

    private String username;

    private String password;

    private String bucket;
}
