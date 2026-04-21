# 快速入门指南

> **目标**: 在 30 分钟内理解 imaping-token 系统并运行第一个示例
> **面向用户**: 新用户、开发者
> **最后更新**: 2026-03-19

---

## 1. 系统简介

**imaping-token** 是一个企业级的 Token 管理和认证系统,提供统一的 Token 生命周期管理、多种存储后端支持和完整的 Spring Security 集成。系统采用模块化架构,支持单机和分布式部署场景,遵循 SOLID 设计原则,易于扩展和维护。

**核心特性:**
- ✅ **Token 生命周期管理** - 创建、存储、检索、更新、删除
- ✅ **灵活的过期策略** - 自动续期、固定时间过期
- ✅ **多存储后端** - 内存存储 (单机) 或 Redis (分布式)
- ✅ **Spring Security 集成** - 开箱即用的认证机制
- ✅ **分布式会话管理** - 支持集群部署和会话共享

---

## 2. 核心概念

### 2.1 Token (令牌)

Token 是系统的核心实体,代表用户的认证凭证。系统当前提供三种 Token 类型:

| Token 类型 | 说明 | 过期策略 | 适用场景 |
|-----------|------|---------|---------|
| **TimeoutAccessToken** | 自动续期访问令牌 | 每次使用自动续期 | 用户会话管理 |
| **RefreshToken** | 续签令牌 | 固定时间后失效,刷新时轮换 | AccessToken 续签 |
| **HardTimeoutToken** | 固定时间令牌 | 固定时间后失效 | 验证码、临时授权链接 |

**Token 核心属性:**
- `id` - Token 唯一标识
- `authentication` - 认证信息 (用户信息、权限等)
- `expirationPolicy` - 过期策略
- `creationTime` - 创建时间
- `lastTimeUsed` - 最后使用时间
- `countOfUses` - 使用次数

当 `imaping.token.accessToken.createAsJwt=true` 时,客户端拿到的 AccessToken 会是签名后的 JWT 字符串；服务端仍保留注册表记录,以支持注销、会话管理和 refresh token 续签。

### 2.2 TokenRegistry (Token 注册表)

TokenRegistry 负责 Token 的存储和检索,提供统一的接口支持多种存储后端:

| 实现类 | 存储方式 | 适用场景 | 持久化 | 集群支持 |
|-------|----------|----------|--------|----------|
| **DefaultTokenRegistry** | ConcurrentHashMap | 单机应用、开发环境 | ❌ | ❌ |
| **RedisTokenRegistry** | Redis | 分布式应用、生产环境 | ✅ | ✅ |

**核心操作:**
```java
// 添加 Token
Token token = tokenRegistry.addToken(token);

// 获取 Token
Token token = tokenRegistry.getToken(tokenId);

// 删除 Token
tokenRegistry.deleteToken(tokenId);

// 获取用户的所有会话
Collection<Token> tokens = tokenRegistry.getSessionsFor(userId);
```

### 2.3 ExpirationPolicy (过期策略)

过期策略决定 Token 何时失效:

**TimeoutExpirationPolicy (自动续期策略):**
- 判断逻辑: `lastTimeUsed + timeToIdle < now`
- 特性: 每次使用自动更新最后使用时间,延长有效期
- 配置参数: `timeToIdle` (空闲超时时间)

**HardTimeoutExpirationPolicy (固定时间策略):**
- 判断逻辑: `creationTime + timeToLive < now`
- 特性: 从创建时间开始计算,到达 TTL 后失效,不受使用影响
- 配置参数: `timeToLive` (存活时间)

---

## 3. Maven 依赖配置

### 3.1 基础依赖 (单机应用 - 内存存储)

适用于开发环境或单机部署场景:

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- imaping-token 核心 API -->
    <dependency>
        <groupId>io.github.imaping</groupId>
        <artifactId>imaping-token-api</artifactId>
        <version>1.6.0</version>
    </dependency>

    <!-- Spring Security 集成 -->
    <dependency>
        <groupId>io.github.imaping</groupId>
        <artifactId>imaping-token-resource-client</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

### 3.2 Redis 存储依赖 (分布式应用)

适用于生产环境或分布式部署场景:

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- imaping-token 核心 API -->
    <dependency>
        <groupId>io.github.imaping</groupId>
        <artifactId>imaping-token-api</artifactId>
        <version>1.6.0</version>
    </dependency>

    <!-- Redis 存储实现 -->
    <dependency>
        <groupId>io.github.imaping</groupId>
        <artifactId>imaping-token-redis-registry</artifactId>
        <version>1.6.0</version>
    </dependency>

    <!-- Spring Security 集成 -->
    <dependency>
        <groupId>io.github.imaping</groupId>
        <artifactId>imaping-token-resource-client</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

---

## 4. 最简配置示例

### 4.1 开发环境配置 (application.yml)

使用内存存储,适用于单机应用:

```yaml
spring:
  application:
    name: my-app
  profiles:
    active: dev

server:
  port: 8080

imaping:
  token:
    accessTokenName: access_token          # Token 参数名
    registry:
      redis:
        enabled: false                     # 禁用 Redis,使用内存存储
      inMemory:
        cache: true                        # 启用 Caffeine 缓存优化
        initialCapacity: 1000              # 初始容量
      cleaner:
        schedule:
          enabled: true                    # 启用定时清理
          repeatInterval: PT2M             # 清理间隔 2 分钟
    accessToken:
      timeToKillInSeconds: 7200            # Token 有效期 2 小时
```

### 4.2 生产环境配置 (application-prod.yml)

使用 Redis 存储,适用于分布式应用:

```yaml
spring:
  application:
    name: my-app
  profiles:
    active: prod
  data:
    redis:
      host: localhost                      # Redis 服务器地址
      port: 6379
      password:                            # Redis 密码 (如需要)
      database: 0
      timeout: 5000
      lettuce:
        pool:
          max-active: 20                   # 最大连接数
          max-idle: 10                     # 最大空闲连接
          min-idle: 5                      # 最小空闲连接

server:
  port: 8080

imaping:
  token:
        accessTokenName: access_token
        registry:
          redis:
            enabled: true                      # 启用 Redis 存储
          core:
            enableLocking: true                # 启用 Redis 分布式锁
          concurrentSessions:
            enabled: true                      # 启用同账号并发会话控制
            maxSessions: 1                     # 单账号只允许 1 个 TimeoutAccessToken 在线
            overflowStrategy: INVALIDATE_OLDEST
            enabledTokenTypes:
              - TimeoutAccessToken
          cleaner:
            schedule:
              enabled: false                   # Redis TTL 自动过期,无需定时清理
        accessToken:
          timeToKillInSeconds: 7200            # Token 有效期 2 小时
```

---

## 5. 五分钟快速运行示例

### 5.1 创建 Spring Boot 应用

**步骤 1: 创建主类**

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TokenDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenDemoApplication.class, args);
    }
}
```

**步骤 2: 创建登录控制器**

```java
package com.example.demo.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    private final TokenRegistry tokenRegistry;
    private final TokenFactory tokenFactory;

    public LoginController(
            @Qualifier(TokenRegistry.BEAN_NAME) TokenRegistry tokenRegistry,
            @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
    }

    /**
     * 登录接口 - 创建 Token
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,
                                     @RequestParam String password) {
        // 1. 验证用户名密码 (这里简化处理)
        if (!"admin".equals(username) || !"123456".equals(password)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "用户名或密码错误");
            return error;
        }

        // 2. 创建用户信息
        BaseUserInfo userInfo = BaseUserInfo.builder()
                .id(1001L)
                .loginName(username)
                .name("管理员")
                .build();

        // 3. 创建认证对象
        Authentication authentication = new Authentication(
                Principal.builder()
                        .id(username)
                        .userInfo(userInfo)
                        .build()
        );

        // 4. 创建 Token
        TimeoutTokenFactory factory = (TimeoutTokenFactory) tokenFactory.get(TimeoutAccessToken.class);
        Token token = factory.create(authentication);
        tokenRegistry.addToken(token);

        // 5. 返回 Token
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("token", token.getId());
        result.put("expiresIn", token.getExpirationPolicy().getTimeToIdle() / 1000);
        return result;
    }

    /**
     * 登出接口 - 删除 Token
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() throws Exception {
        String currentToken = SecurityContextUtil.getCurrentToken();
        tokenRegistry.deleteToken(currentToken);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    public Object getUserInfo() throws Exception {
        return SecurityContextUtil.getCurrentUserInfo();
    }
}
```

**步骤 3: 创建受保护资源控制器**

```java
package com.example.demo.controller;

import io.github.imaping.token.core.util.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * 受保护的资源接口
     * 需要 Token 才能访问
     */
    @GetMapping("/protected")
    public Map<String, Object> protectedResource() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "这是受保护的资源");
        result.put("currentUser", SecurityContextUtil.getCurrentUserInfo());
        return result;
    }

    /**
     * 公开资源接口
     * 无需 Token 即可访问
     */
    @GetMapping("/public")
    public Map<String, Object> publicResource() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "这是公开资源,无需认证");
        return result;
    }
}
```

**步骤 4: 配置安全策略 (可选)**

如需自定义哪些路径需要认证,可创建配置类:

```java
package com.example.demo.config;

import io.github.imaping.token.resource.client.config.TokenSecurityConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig extends TokenSecurityConfig {

    @Override
    protected String[] getPermitAntMatchers() {
        // 无需认证的路径
        return new String[]{
                "/login",
                "/api/public",
                "/health",
                "/error"
        };
    }

    @Override
    protected String[] getAuthenticatedAntMatchers() {
        // 需要认证的路径
        return new String[]{
                "/api/**",
                "/user-info",
                "/logout"
        };
    }
}
```

### 5.2 配置文件

创建 `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: token-demo

server:
  port: 8080

imaping:
  token:
        accessTokenName: access_token
        registry:
          redis:
            enabled: false
          cleaner:
            schedule:
              enabled: true
              repeatInterval: PT2M
        accessToken:
          timeToKillInSeconds: 7200
```

### 5.3 运行应用

**启动应用:**

```bash
mvn spring-boot:run
```

或者直接运行 `TokenDemoApplication` 主类。

**测试 API:**

```bash
# 1. 登录获取 Token
curl -X POST "http://localhost:8080/login?username=admin&password=123456"

# 响应示例:
# {
#   "success": true,
#   "token": "AT-1-abcd1234efgh5678ijkl9012mnop3456",
#   "expiresIn": 7200
# }

# 2. 使用 Token 访问受保护资源 (在 Header 中传递)
curl -H "access_token: AT-1-abcd1234efgh5678ijkl9012mnop3456" \
     http://localhost:8080/api/protected

# 3. 使用 Token 访问受保护资源 (在 URL 参数中传递)
curl "http://localhost:8080/api/protected?access_token=AT-1-abcd1234efgh5678ijkl9012mnop3456"

# 4. 获取当前用户信息
curl -H "access_token: AT-1-abcd1234efgh5678ijkl9012mnop3456" \
     http://localhost:8080/user-info

# 5. 登出
curl -X POST -H "access_token: AT-1-abcd1234efgh5678ijkl9012mnop3456" \
     http://localhost:8080/logout
```

**Token 传递方式:**

当前默认推荐的传递方式如下:

1. **AccessToken**: `Authorization: Bearer <token>` 或 `access_token: <token>`
2. **AccessToken Cookie**: `access_token=<token>`
3. **RefreshToken Cookie**: `refresh_token=<token>`，默认用于 `/refresh` 自动续签

默认情况下,AccessToken 的 URL 参数传递是关闭的,需要显式配置后才可使用。

---

## 6. 常见问题

### Q1: 如何切换到 Redis 存储?

**A**: 只需修改配置并添加 Redis 依赖:

1. 添加 Maven 依赖:
```xml
<dependency>
    <groupId>io.github.imaping</groupId>
    <artifactId>imaping-token-redis-registry</artifactId>
    <version>1.6.0</version>
</dependency>
```

2. 修改配置:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

imaping:
  token:
        registry:
          redis:
            enabled: true  # 启用 Redis
```

### Q2: 如何自定义 Token 有效期?

**A**: 修改配置中的 `timeToKillInSeconds`:

```yaml
imaping:
  token:
        accessToken:
          timeToKillInSeconds: 3600  # 1 小时
```

### Q3: 如何获取当前登录用户信息?

**A**: 使用 `SecurityContextUtil` 工具类:

```java
import io.github.imaping.token.core.util.SecurityContextUtil;

// 获取用户信息
BaseUserInfo userInfo = SecurityContextUtil.getCurrentUserInfo();

// 获取用户 ID
String userId = SecurityContextUtil.getUserId();

// 获取当前 Token
String token = SecurityContextUtil.getCurrentToken();
```

### Q4: 如何实现单账号单登?

**A**: 在需要受控的 Token 类型上开启并发会话控制即可。当前默认只控制 `TimeoutAccessToken`, `HardTimeoutToken` 默认不受影响。

```yaml
imaping:
  token:
        registry:
          redis:
            enabled: true
          core:
            enableLocking: true
          concurrentSessions:
            enabled: true
            maxSessions: 1
            overflowStrategy: INVALIDATE_OLDEST
            enabledTokenTypes:
              - TimeoutAccessToken
```

多实例部署时必须让所有实例共享同一个 Redis,并保持 `enableLocking=true`,这样单账号单登才能在全局生效。

### Q5: RefreshToken 如何使用?

**A**: 默认登录成功后会同时签发 AccessToken 和 RefreshToken:

- AccessToken 用于访问业务接口
- RefreshToken 默认写入 `HttpOnly` Cookie: `refresh_token`
- 调用 `/refresh` 时,如果请求参数未显式提供 `refreshToken`,框架会自动从 Cookie 读取并轮换签发新的一对 Token

### Q6: Token 过期后会自动删除吗?

**A**: 取决于存储后端:

- **内存存储**: 需要定时清理任务 (`imaping.token.registry.cleaner.schedule.enabled=true`)
- **Redis 存储**: 自动过期 (TTL 机制),无需定时清理

---

## 7. 下一步

现在你已经成功运行第一个 imaping-token 示例!接下来可以:

- 📖 **阅读架构文档**: [architecture.md](architecture.md) - 深入理解系统设计
- 🔧 **配置参考**: [configuration.md](configuration.md) - 完整配置项说明 *(即将推出)*
- 🚀 **API 使用指南**: [api-guide.md](api-guide.md) - 高级功能和最佳实践 *(即将推出)*
- 🔌 **集成指南**: [integration.md](integration.md) - 与其他系统集成 *(即将推出)*
- 💡 **最佳实践**: [best-practices.md](best-practices.md) - 生产环境部署建议 *(即将推出)*

---

**文档维护**: imaping-token 团队
**问题反馈**: 请通过项目 Issue 提交
**最后更新**: 2025-10-12





