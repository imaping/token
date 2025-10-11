package com.imaping.token.configuration.model.attachment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

/**
 * @author miaoj
 */
@Getter
@Setter
@Accessors(chain = true)
public class AttachmentProperties implements Serializable {

    private static final long serialVersionUID = 5595871874286081060L;
    @NestedConfigurationProperty
    private MinioProperties minio = new MinioProperties();

    @NestedConfigurationProperty
    private SftpProperties sftp = new SftpProperties();
}
