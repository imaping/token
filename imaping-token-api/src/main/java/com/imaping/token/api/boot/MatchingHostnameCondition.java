package com.imaping.token.api.boot;

import com.imaping.token.api.common.FunctionUtils;
import com.imaping.token.api.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.net.InetAddress;

public class MatchingHostnameCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String name = metadata.getAnnotationAttributes(ConditionalOnMatchingHostname.class.getName()).get("name").toString();
        String hostnameToMatch = context.getEnvironment().getProperty(name);
        if (StringUtils.isBlank(hostnameToMatch)) {
            return ConditionOutcome.match("No hostname set with property: " + name);
        }
        if (RegexUtils.find(hostnameToMatch, getCasServerHostName())) {
            return ConditionOutcome.match("Hostname matches value for " + name);
        }
        return ConditionOutcome.noMatch("Hostname doesn't match value for " + name);
    }

    public static String getCasServerHostName() {
        return FunctionUtils.doAndHandle(() -> {
            String hostName = InetAddress.getLocalHost().getHostName();
            int index = hostName.indexOf('.');
            if (index > 0) {
                return hostName.substring(0, index);
            }
            return hostName;
        }, throwable -> "unknown").get();
    }
}
