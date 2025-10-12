package com.imaping.token.configuration.model.attachment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 附件配置属性类.
 *
 * <p>配置类仅在内存中使用,无需序列化.</p>
 *
 * @author miaoj
 * @since 0.0.1
 */
@Getter
@Setter
@Accessors(chain = true)
public class AttachmentProperties {

    @NestedConfigurationProperty
    private MinioProperties minio = new MinioProperties();

    @NestedConfigurationProperty
    private SftpProperties sftp = new SftpProperties();
}
