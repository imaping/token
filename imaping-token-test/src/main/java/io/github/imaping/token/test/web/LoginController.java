package io.github.imaping.token.test.web;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.factory.TimeoutTokenFactory;
import io.github.imaping.token.api.factory.TokenFactory;
import io.github.imaping.token.api.model.TimeoutAccessToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.api.registry.TokenRegistry;
import io.github.imaping.token.core.model.BaseUserInfo;
import io.github.imaping.token.core.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 不使用单点登录，自定义登录
 */
@RestController
public class LoginController {

    private final TokenRegistry tokenRegistry;

    private final TokenFactory tokenFactory;

    public LoginController(@Qualifier(TokenRegistry.BEAN_NAME) TokenRegistry tokenRegistry,
                           @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
    }


    @GetMapping("/rest/user-info")
    public Object userInfo() throws Exception {
        return SecurityContextUtil.getCurrentUserInfo();
    }


    @PostMapping("/login")
    public Object login(String username, String password) throws Exception {
        //todo: 自定义验证用户名密码，验证成功后根据用户信息生产token
        final Authentication<String> authentication = new Authentication<>(
                Principal.<String>builder()
                        .id(username)
                        .userInfo(
                                BaseUserInfo.<String>builder()
                                        .name("test")
                                        .build())
                        .build());
        final TimeoutTokenFactory factory = (TimeoutTokenFactory) tokenFactory.get(TimeoutAccessToken.class);
        Token token = factory.create(authentication);
        tokenRegistry.addToken(token);
        return token;
    }

    @PostMapping("/logout")
    public Object logout() throws Exception {
        //前端删除token、后端删除token 即可
        final String currentToken = SecurityContextUtil.getCurrentToken();
        tokenRegistry.deleteToken(currentToken);
        return true;
    }
}
