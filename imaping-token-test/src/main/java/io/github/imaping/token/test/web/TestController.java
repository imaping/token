package io.github.imaping.token.test.web;

import io.github.imaping.token.api.session.TokenSessionService;
import io.github.imaping.token.core.util.SecurityContextUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rest/business")
public class TestController {
    private final TokenSessionService tokenSessionService;

    public TestController(final TokenSessionService tokenSessionService) {
        this.tokenSessionService = tokenSessionService;
    }

    @GetMapping("/security")
    public Object test() throws Exception {
        return "test";
    }

    @GetMapping("/userinfo")
    public Object userinfo() {
        return SecurityContextUtil.getCurrentUserInfo();
    }

    @GetMapping("/workflow")
    public Object workflow() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 1);
        Map<String, Object> content = new HashMap<>();
        content.put("userName", "admin");
        result.put("content", content);
        return result;
    }

    @GetMapping("/sessions")
    public Object sessions() {
        return tokenSessionService.getSessionsFor(SecurityContextUtil.getUserId(), SecurityContextUtil.getCurrentToken());
    }

    @DeleteMapping("/sessions/{tokenId}")
    public Object revokeSession(@PathVariable("tokenId") final String tokenId) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", tokenSessionService.revokeSession(SecurityContextUtil.getUserId(), tokenId));
        return result;
    }

    @DeleteMapping("/sessions/current/others")
    public Object revokeOtherSessions() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", tokenSessionService.revokeOtherSessions(SecurityContextUtil.getUserId(), SecurityContextUtil.getCurrentToken()));
        return result;
    }
}
