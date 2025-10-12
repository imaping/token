package com.imaping.token.configuration;

import com.imaping.token.configuration.model.attachment.AttachmentProperties;
import com.imaping.token.configuration.model.cas.CasProperties;
import com.imaping.token.configuration.model.district.DistrictProperties;
import com.imaping.token.configuration.model.elasticsearch.ElasticSearchProperties;
import com.imaping.token.configuration.model.token.CaptchaProperties;
import com.imaping.token.configuration.model.token.CloudProperties;
import com.imaping.token.configuration.model.token.TokenConfigurationProperties;
import com.imaping.token.configuration.model.user.UserProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * IMapping 主配置属性类.
 *
 * <p>绑定 {@code imaping.*} 配置项,由 Spring Boot 在应用启动时加载.</p>
 * <p>配置类仅在内存中使用,由 Spring 容器管理,无需序列化.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "imaping")
@Getter
@Setter
@Accessors(chain = true)
public class IMapingConfigurationProperties {

    @NestedConfigurationProperty
    private TokenConfigurationProperties token = new TokenConfigurationProperties();

    @NestedConfigurationProperty
    private CloudProperties cloud = new CloudProperties();

    @NestedConfigurationProperty
    private CaptchaProperties captcha = new CaptchaProperties();

    @NestedConfigurationProperty
    private AttachmentProperties attachment = new AttachmentProperties();

    @NestedConfigurationProperty
    private CasProperties cas = new CasProperties();

    @NestedConfigurationProperty
    private ElasticSearchProperties elastic = new ElasticSearchProperties();

    @NestedConfigurationProperty
    private DistrictProperties district = new DistrictProperties();

    @NestedConfigurationProperty
    private UserProperties user = new UserProperties();
}
