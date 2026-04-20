package io.github.imaping.token.api.refresh;

import io.github.imaping.token.api.authentication.Authentication;

public interface TokenRefreshService {
    String BEAN_NAME = "tokenRefreshService";

    boolean isEnabled();

    TokenGrant issue(Authentication<?> authentication) throws Exception;

    TokenGrant refresh(String refreshTokenId) throws Exception;

    long revokeGrant(String refreshTokenId) throws Exception;
}
