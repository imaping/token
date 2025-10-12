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

import java.io.Serial;
import java.io.Serializable;

@ConfigurationProperties(prefix = "imaping")
@Getter
@Setter
@Accessors(chain = true)
public class IMapingConfigurationProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = -5924836083122042076L;

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
