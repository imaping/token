package com.imaping.token.api.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import java.io.Serializable;

@Getter
@ToString
public class TokenError implements Serializable {

    /**
     * {@code invalid_request} - The request is missing a required parameter, includes an unsupported parameter or
     * parameter value, repeats the same parameter, uses more than one method for including an access token, or is
     * otherwise malformed.
     */
    public static String INVALID_REQUEST = "invalid_request";

    /**
     * {@code invalid_token} - The access token provided is expired, revoked, malformed, or invalid for other
     * reasons.
     */
    public static String INVALID_TOKEN = "invalid_token";

    private static final long serialVersionUID = 2157110672977988758L;

    private final String errorCode;

    private final String description;

    @JsonIgnore
    private final HttpStatus httpStatus;

    public TokenError(String errorCode) {
        this(errorCode, null, null);
    }

    /**
     * Constructs an {@code OAuth2Error} using the provided parameters.
     *
     * @param errorCode   the error code
     * @param httpStatus  httpStatus
     * @param description the error description
     */
    public TokenError(String errorCode, HttpStatus httpStatus, String description) {
        Assert.hasText(errorCode, "errorCode cannot be empty");
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.description = description;
    }
}
