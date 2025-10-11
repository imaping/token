package com.imaping.token.api.boot;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(MatchingHostnameCondition.class)
public @interface ConditionalOnMatchingHostname {
    /**
     * Name of the property containing the hostname to
     * match as its value (may be a Java regular expression).
     *
     * @return the pattern or the host name.
     */
    String name();

}