package com.imaping.token.configuration.model.attachment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * SFTP 配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class SftpProperties {

    private String host;

    private int port;

    private String username;

    private String password;

    private String root;
}
