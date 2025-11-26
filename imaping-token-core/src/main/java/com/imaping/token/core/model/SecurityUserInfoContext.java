package com.imaping.token.core.model;

/**
 * @author miaoj
 */
public interface SecurityUserInfoContext<ID extends java.io.Serializable> {

    String BEAN_NAME = "securityUserInfoContext";


    SecurityUserInfo<ID> getCurrentUserInfo();
}
