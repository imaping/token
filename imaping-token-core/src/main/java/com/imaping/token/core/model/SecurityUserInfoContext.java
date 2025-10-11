package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public interface SecurityUserInfoContext {

    String BEAN_NAME = "securityUserInfoContext";


    SecurityUserInfo getCurrentUserInfo();
}
