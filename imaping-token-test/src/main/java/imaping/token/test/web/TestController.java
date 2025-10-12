package imaping.token.test.web;

import com.imaping.token.core.util.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rest/business")
public class TestController {
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
}
