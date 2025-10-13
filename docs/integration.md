# 集成指南

> **快速参考**: imaping-token 系统集成步骤和部署配置
> **最后更新**: 2025-10-12
> **适用版本**: 0.0.2-SNAPSHOT

---

## 目录

1. [Spring Security 集成](#1-spring-security-集成)
2. [Redis 配置和优化](#2-redis-配置和优化)
3. [存储选择建议](#3-存储选择建议)
4. [多实例部署配置](#4-多实例部署配置)
5. [Spring Boot Actuator 集成](#5-spring-boot-actuator-集成)
6. [常见集成场景](#6-常见集成场景)

---

## 1. Spring Security 集成

### 1.1 添加依赖配置

#### Maven 配置

在现有 Spring Security 项目的 `pom.xml` 中添加依赖:

```xml
<dependencies>
    <!-- imaping-token 核心依赖 -->
    <dependency>
        <groupId>com.imaping</groupId>
        <artifactId>imaping-token-resource-client</artifactId>
        <version>0.0.2-SNAPSHOT</version>
    </dependency>

    <!-- imaping-token Redis 存储(可选,分布式场景需要) -->
    <dependency>
        <groupId>com.imaping</groupId>
        <artifactId>imaping-token-redis-registry</artifactId>
        <version>0.0.2-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**说明**:
- `imaping-token-resource-client`: 包含 Spring Security 集成和 Token 认证过滤器
- `imaping-token-redis-registry`: Redis 存储实现,单机部署可省略(使用内存存储)

#### Gradle 配置

```groovy
dependencies {
    // imaping-token 核心依赖
    implementation 'com.imaping:imaping-token-resource-client:0.0.2-SNAPSHOT'

    // imaping-token Redis 存储(可选)
    implementation 'com.imaping:imaping-token-redis-registry:0.0.2-SNAPSHOT'
}
```

---

### 1.2 自定义 Security 配置

#### 方式一: 扩展 TokenSecurityConfig(推荐)

创建自定义配置类继承 `TokenSecurityConfig`:

```java
package com.example.config;

import com.imaping.token.resource.client.config.TokenSecurityConfig;
import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Token Security 配置
 *
 * <p>通过继承 TokenSecurityConfig 并重写钩子方法,自定义路径访问控制规则</p>
 */
@Configuration
@EnableWebSecurity
public class CustomSecurityConfig extends TokenSecurityConfig {

    public CustomSecurityConfig(
            TokenRegistry tokenRegistry,
            @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory,
            IMapingConfigurationProperties properties) {
        super(tokenRegistry, tokenFactory, properties);
    }

    /**
     * 配置无需认证的路径
     */
    @Override
    protected String[] getPermitAntMatchers() {
        return new String[]{
            "/public/**",           // 公共资源
            "/login",               // 登录接口
            "/logout",              // 登出接口
            "/health",              // 健康检查
            "/actuator/health",     // Actuator 健康检查
            "/error"                // 错误页面
        };
    }

    /**
     * 配置需要认证的路径
     */
    @Override
    protected String[] getAuthenticatedAntMatchers() {
        return new String[]{
            "/api/**",              // API 接口
            "/admin/**"             // 管理后台
        };
    }

    /**
     * 配置需要认证的路径(按 HTTP 方法)
     */
    @Override
    protected Map<HttpMethod, String[]> getAuthenticatedAntMatchersWithMethod() {
        Map<HttpMethod, String[]> map = new HashMap<>();
        // 所有 POST 请求需要认证
        map.put(HttpMethod.POST, new String[]{"/api/**"});
        // 所有 PUT 请求需要认证
        map.put(HttpMethod.PUT, new String[]{"/api/**"});
        // 所有 DELETE 请求需要认证
        map.put(HttpMethod.DELETE, new String[]{"/api/**"});
        return map;
    }

    /**
     * 配置无需认证的路径(按 HTTP 方法)
     */
    @Override
    protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
        Map<HttpMethod, String[]> map = new HashMap<>();
        // 所有 GET 请求公开访问(除了 /api/** 和 /admin/**)
        map.put(HttpMethod.GET, new String[]{"/docs/**", "/swagger-ui/**"});
        // OPTIONS 请求允许访问(CORS 预检请求)
        map.put(HttpMethod.OPTIONS, new String[]{"/**"});
        return map;
    }

    /**
     * 其他请求是否需要认证
     *
     * @return true - 所有其他请求需要认证(默认); false - 其他请求允许访问
     */
    @Override
    protected boolean isAnyRequestAuthenticated() {
        return true; // 默认其他请求需要认证
    }
}
```

**关键说明**:
- **`@ConditionalOnMissingBean`**: 原始 `TokenSecurityConfig` 有此注解,当自定义配置存在时会自动禁用默认配置
- **钩子方法**: 通过重写钩子方法实现自定义,无需重写整个 `SecurityFilterChain`
- **路径匹配**: 使用 Ant 风格路径模式 (`**` 匹配多级路径, `*` 匹配单级)

---

#### 方式二: 完全自定义 SecurityFilterChain

如果需要更复杂的定制,可以完全自定义 `SecurityFilterChain`:

```java
package com.example.config;

import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.configuration.IMapingConfigurationProperties;
import com.imaping.token.resource.client.authentication.TokenAuthenticationEntryPoint;
import com.imaping.token.resource.client.authentication.TokenAuthenticationProvider;
import com.imaping.token.resource.client.filter.TokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 完全自定义的 Security 配置
 *
 * <p>适用于需要更复杂控制的场景,例如多个 SecurityFilterChain、自定义认证流程等</p>
 */
@Configuration
@EnableWebSecurity
public class AdvancedSecurityConfig {

    private final TokenRegistry tokenRegistry;
    private final TokenFactory tokenFactory;
    private final IMapingConfigurationProperties properties;

    public AdvancedSecurityConfig(
            TokenRegistry tokenRegistry,
            @Qualifier(TokenFactory.BEAN_NAME) TokenFactory tokenFactory,
            IMapingConfigurationProperties properties) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
        this.properties = properties;
    }

    @Bean
    public TokenAuthenticationProvider tokenAuthenticationProvider() {
        return new TokenAuthenticationProvider(tokenRegistry);
    }

    @Bean
    public TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint() {
        return new TokenAuthenticationEntryPoint();
    }

    /**
     * API 接口的 SecurityFilterChain
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")  // 仅匹配 /api/** 路径
            .authenticationProvider(tokenAuthenticationProvider())
            .sessionManagement(configurer ->
                configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(registry ->
                registry.anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .httpBasic(configurer ->
                configurer.authenticationEntryPoint(tokenAuthenticationEntryPoint()));

        // 添加 Token 认证过滤器
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        http.addFilterBefore(
            new TokenAuthenticationFilter(authenticationManager, tokenAuthenticationEntryPoint(), properties),
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * 公共资源的 SecurityFilterChain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/public/**", "/login", "/health")
            .authorizeHttpRequests(registry ->
                registry.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

**关键说明**:
- **`@Order`**: 多个 `SecurityFilterChain` 时需要指定顺序,数字越小优先级越高
- **`securityMatcher()`**: 指定此 FilterChain 匹配的路径范围
- **无状态会话**: `SessionCreationPolicy.STATELESS` 适用于基于 Token 的认证

---

### 1.3 Token 认证过滤器配置说明

`TokenAuthenticationFilter` 是核心认证过滤器,自动从 HTTP 请求中提取 Token 并验证。

#### Token 提取优先级

1. **HTTP Header** (最高优先级): `Authorization: Bearer <token>`
2. **Cookie**: `access_token=<token>` (Cookie 名称可配置)
3. **Request Parameter** (最低优先级): `?access_token=<token>`

#### 配置 Token 名称

```yaml
imaping:
  token:
    accessTokenName: access_token  # 默认值,可自定义
```

#### 前端集成示例

**方式一: HTTP Header (推荐,适用于 API 调用)**

```javascript
// JavaScript/Axios 示例
axios.get('/api/users', {
  headers: {
    'Authorization': 'Bearer ' + token
  }
});

// Fetch API 示例
fetch('/api/users', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

**方式二: Cookie (推荐,适用于浏览器会话)**

```java
// 后端登录接口设置 Cookie
@PostMapping("/login")
public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
    // 验证用户名密码...
    Token token = tokenFactory.createToken(authentication);

    // 设置 Cookie
    Cookie cookie = new Cookie("access_token", token.getId());
    cookie.setHttpOnly(true);    // 防止 XSS 攻击
    cookie.setSecure(true);       // 仅 HTTPS 传输
    cookie.setPath("/");          // Cookie 作用范围
    cookie.setMaxAge(7200);       // 2 小时
    response.addCookie(cookie);

    return ResponseEntity.ok().build();
}
```

**方式三: Request Parameter (不推荐,仅用于测试)**

```
GET /api/users?access_token=your_token_here
```

⚠️ **安全警告**: URL 参数会被记录在日志中,不适合生产环境。

---

### 1.4 路径访问权限配置示例

#### 场景一: RESTful API 项目

```java
@Override
protected String[] getPermitAntMatchers() {
    return new String[]{
        "/api/v1/auth/**",      // 认证相关接口
        "/api/v1/public/**",    // 公共 API
        "/actuator/health"      // 健康检查
    };
}

@Override
protected String[] getAuthenticatedAntMatchers() {
    return new String[]{
        "/api/v1/users/**",     // 用户管理
        "/api/v1/orders/**"     // 订单管理
    };
}
```

#### 场景二: 前后端分离项目

```java
@Override
protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
    Map<HttpMethod, String[]> map = new HashMap<>();
    // OPTIONS 请求允许(CORS 预检)
    map.put(HttpMethod.OPTIONS, new String[]{"/**"});
    // GET 请求部分开放
    map.put(HttpMethod.GET, new String[]{"/api/articles/**", "/api/categories/**"});
    return map;
}

@Override
protected Map<HttpMethod, String[]> getAuthenticatedAntMatchersWithMethod() {
    Map<HttpMethod, String[]> map = new HashMap<>();
    // POST/PUT/DELETE 需要认证
    map.put(HttpMethod.POST, new String[]{"/api/**"});
    map.put(HttpMethod.PUT, new String[]{"/api/**"});
    map.put(HttpMethod.DELETE, new String[]{"/api/**"});
    return map;
}
```

#### 场景三: 管理后台 + API 混合项目

```java
@Override
protected String[] getPermitAntMatchers() {
    return new String[]{
        "/",                    // 首页
        "/login",               // 登录页
        "/assets/**",           // 静态资源
        "/error"                // 错误页
    };
}

@Override
protected String[] getAuthenticatedAntMatchers() {
    return new String[]{
        "/admin/**",            // 后台管理
        "/api/private/**"       // 私有 API
    };
}

@Override
protected boolean isAnyRequestAuthenticated() {
    return true; // 其他所有请求需要认证
}
```

---

### 1.5 完整集成示例代码

#### 项目结构

```
src/main/java/com/example/
├── config/
│   └── CustomSecurityConfig.java      # Security 配置
├── controller/
│   ├── LoginController.java           # 登录控制器
│   └── UserController.java            # 用户控制器
└── Application.java                    # 启动类
```

#### LoginController.java

```java
package com.example.controller;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.TokenRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 *
 * <p>提供用户登录和登出功能,创建和删除 Token</p>
 */
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final TokenRegistry tokenRegistry;

    @Qualifier(TokenFactory.BEAN_NAME)
    private final TokenFactory tokenFactory;

    /**
     * 用户登录
     *
     * @param request 登录请求(用户名、密码)
     * @param response HTTP 响应,用于设置 Cookie
     * @return Token 信息
     */
    @PostMapping
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        // 1. 验证用户名和密码(这里简化处理,实际应查询数据库)
        if (!"admin".equals(request.getUsername()) ||
            !"password".equals(request.getPassword())) {
            return ResponseEntity.status(401).build();
        }

        // 2. 创建认证信息
        Authentication authentication = new Authentication(
            request.getUsername(),
            null  // 密码不应存储在 Token 中
        );
        authentication.addAttribute("userId", "12345");
        authentication.addAttribute("role", "ADMIN");

        // 3. 创建 Token
        Token token = tokenFactory.createToken(authentication);

        // 4. 添加到注册表(如果工厂未自动添加)
        // tokenRegistry.addToken(token);  // 可选,TokenFactory 通常已添加

        // 5. 设置 Cookie(推荐方式)
        Cookie cookie = new Cookie("access_token", token.getId());
        cookie.setHttpOnly(true);     // 防止 XSS
        cookie.setSecure(true);        // 仅 HTTPS(生产环境)
        cookie.setPath("/");
        cookie.setMaxAge(7200);        // 2 小时
        // cookie.setAttribute("SameSite", "Strict"); // Spring Boot 2.6+ 支持
        response.addCookie(cookie);

        // 6. 返回 Token 信息
        return ResponseEntity.ok(new LoginResponse(token.getId(), 7200L));
    }

    /**
     * 用户登出
     *
     * @param tokenId Token ID(从 Header 或 Cookie 提取)
     * @param response HTTP 响应,用于清除 Cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken,
            HttpServletResponse response) {

        // 1. 提取 Token ID
        String tokenId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenId = authHeader.substring(7);
        } else if (cookieToken != null) {
            tokenId = cookieToken;
        }

        // 2. 删除 Token
        if (tokenId != null) {
            tokenRegistry.deleteToken(tokenId);
        }

        // 3. 清除 Cookie
        Cookie cookie = new Cookie("access_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 立即过期
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }
}

// DTO 类
class LoginRequest {
    private String username;
    private String password;
    // getters and setters...
}

class LoginResponse {
    private String token;
    private Long expiresIn;

    public LoginResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
    // getters and setters...
}
```

---

## 2. Redis 配置和优化

### 2.1 Redis 基础配置(单机模式)

#### 启用 Redis 存储

在 `application.yml` 中配置:

```yaml
# imaping-token 配置
imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: true              # ✅ 启用 Redis 存储
      core:
        enable-locking: false      # 单机模式无需分布式锁
    accessToken:
      timeToKillInSeconds: 7200    # Token 过期时间 2 小时
    scheduling:
      enabled: false               # Redis 自动过期,无需定时清理

# Spring Data Redis 配置
spring:
  data:
    redis:
      host: localhost              # Redis 服务器地址
      port: 6379                   # Redis 端口
      password:                    # Redis 密码(无密码留空)
      database: 0                  # 数据库索引
      timeout: 5000ms              # 连接超时时间

      # Lettuce 连接池配置(推荐)
      lettuce:
        pool:
          max-active: 8            # 最大连接数
          max-idle: 8              # 最大空闲连接
          min-idle: 2              # 最小空闲连接
          max-wait: 2000ms         # 最大等待时间
```

#### Properties 格式(与上面 YAML 等效)

```properties
# imaping-token 配置
imaping.token.accessTokenName=access_token
imaping.token.registry.redis.enabled=true
imaping.token.registry.core.enable-locking=false
imaping.token.accessToken.timeToKillInSeconds=7200
imaping.token.scheduling.enabled=false

# Spring Data Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=5000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.max-wait=2000ms
```

---

### 2.2 Redis 集群配置(Cluster 模式)

Redis Cluster 模式适用于大规模分布式部署,提供自动分片和高可用性。

```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true       # ✅ 启用分布式锁

spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node1.example.com:6379
          - redis-node2.example.com:6379
          - redis-node3.example.com:6379
          - redis-node4.example.com:6379
          - redis-node5.example.com:6379
          - redis-node6.example.com:6379
        max-redirects: 3           # 最大重定向次数
      password: ${REDIS_PASSWORD}  # 使用环境变量
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20           # 集群模式增加连接数
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
```

**配置说明**:
- **`nodes`**: 集群节点列表,至少 6 个节点(3 主 3 从)
- **`max-redirects`**: 当 Key 不在当前节点时的最大重定向次数
- **`enable-locking`**: 集群模式必须启用分布式锁,防止并发冲突

---

### 2.3 Redis 哨兵配置(Sentinel 模式)

Redis Sentinel 模式提供主从复制和自动故障转移。

```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true       # 启用分布式锁

spring:
  data:
    redis:
      sentinel:
        master: mymaster           # 主节点名称
        nodes:
          - sentinel1.example.com:26379
          - sentinel2.example.com:26379
          - sentinel3.example.com:26379
        password: ${REDIS_PASSWORD}
      password: ${REDIS_PASSWORD}  # Redis 主从密码
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 15
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
```

**配置说明**:
- **`master`**: 主节点在 Sentinel 中的名称(配置 Sentinel 时定义)
- **`nodes`**: Sentinel 节点列表,至少 3 个节点(奇数个)
- **`password`**: 如果 Sentinel 和 Redis 都有密码,需要分别配置

---

### 2.4 Lettuce 连接池优化参数说明

| 参数 | 说明 | 推荐值(单机) | 推荐值(集群) | 推荐值(高并发) |
|------|------|-------------|-------------|---------------|
| **max-active** | 最大连接数 | 8 | 20 | 50+ |
| **max-idle** | 最大空闲连接 | 8 | 10 | 20 |
| **min-idle** | 最小空闲连接 | 2 | 5 | 10 |
| **max-wait** | 最大等待时间(ms) | 2000 | 2000 | 3000 |
| **time-between-eviction-runs** | 空闲连接检查间隔 | - | 60000ms | 60000ms |

#### 性能调优建议

**低并发场景(< 100 QPS)**:
```yaml
lettuce:
  pool:
    max-active: 8
    max-idle: 8
    min-idle: 2
    max-wait: 2000ms
```

**中并发场景(100-1000 QPS)**:
```yaml
lettuce:
  pool:
    max-active: 20
    max-idle: 15
    min-idle: 5
    max-wait: 2000ms
```

**高并发场景(> 1000 QPS)**:
```yaml
lettuce:
  pool:
    max-active: 50
    max-idle: 30
    min-idle: 10
    max-wait: 3000ms
```

**性能优化技巧**:
1. **`max-active`**: 设置为峰值并发数的 1.5 倍
2. **`min-idle`**: 保持一定数量的空闲连接,避免频繁创建连接
3. **`max-wait`**: 不宜过长,避免请求积压;不宜过短,避免过早失败
4. **监控连接池**: 使用 Actuator 监控 Redis 连接池状态

---

### 2.5 Redis Key 格式和 TTL 策略

#### Key 格式

imaping-token 在 Redis 中使用以下 Key 格式:

```
imaping.token:{tokenId}:{userId}
```

**示例**:
```
imaping.token:4xY7k9zP2mQ1nR3s:user12345
```

**说明**:
- **`imaping.token`**: 统一前缀,便于识别和管理
- **`{tokenId}`**: Token 的唯一标识符
- **`{userId}`**: 用户 ID,便于按用户查询

#### TTL 自动过期策略

Redis 存储自动利用 TTL(Time To Live)机制实现 Token 过期:

```yaml
imaping:
  token:
    accessToken:
      timeToKillInSeconds: 7200  # 设置 TTL = 7200 秒(2 小时)
```

**工作原理**:
1. Token 创建时,Redis 自动设置 Key 的 TTL
2. TTL 到期后,Redis 自动删除 Key
3. 无需应用层定时清理任务

**验证 TTL**:
```bash
# 查看 Token 剩余过期时间(秒)
redis-cli TTL "imaping.token:4xY7k9zP2mQ1nR3s:user12345"
# 输出: 3600 (还剩 1 小时)
```

---

## 3. 存储选择建议

### 3.1 内存存储 vs Redis 存储对比

| 对比项 | 内存存储 (ConcurrentHashMap) | Redis 存储 |
|--------|------------------------------|-----------|
| **使用场景** | 单机应用、开发环境、测试环境 | 分布式应用、生产环境、多实例部署 |
| **性能** | ⚡ 极快(纳秒级) | ⚡ 快(微秒级,网络延迟) |
| **持久化** | ❌ 无持久化,重启丢失 | ✅ 支持 RDB + AOF 持久化 |
| **集群支持** | ❌ 不支持,仅单机 | ✅ 支持主从复制、Sentinel、Cluster |
| **TTL 过期** | 需要定时任务清理 | ✅ Redis 自动过期,无需应用层处理 |
| **高可用** | ❌ 单点故障 | ✅ 支持主从切换、故障转移 |
| **资源消耗** | 消耗应用 JVM 内存 | 独立 Redis 进程,不占用应用内存 |
| **外部依赖** | ✅ 无外部依赖 | ❌ 需要 Redis 服务 |
| **扩展性** | ❌ 受限于单机内存 | ✅ 可横向扩展(集群分片) |
| **配置开关** | `imaping.token.registry.redis.enabled=false` | `imaping.token.registry.redis.enabled=true` |

---

### 3.2 使用场景分析

#### 场景一: 单机开发/测试环境

**推荐**: 内存存储

**配置**:
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: false             # ❌ 禁用 Redis
      inMemory:
        cache: true                # ✅ 启用 Caffeine 缓存优化
        initialCapacity: 1000      # 初始容量
    scheduling:
      enabled: true                # ✅ 启用定时清理
      repeatInterval: 120000       # 2 分钟清理一次
```

**优势**: 无需启动 Redis,开发调试方便

---

#### 场景二: 单机生产环境(小流量)

**推荐**: 内存存储 + 定期备份

**配置**:
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: false
      inMemory:
        cache: true
        initialCapacity: 5000      # 根据用户量调整
    scheduling:
      enabled: true
      repeatInterval: 60000        # 1 分钟清理一次
```

**适用条件**:
- 用户量 < 1000
- 并发请求 < 100 QPS
- 可接受应用重启时 Token 失效(用户需重新登录)

---

#### 场景三: 分布式生产环境(中高流量)

**推荐**: Redis 存储(单机或主从)

**配置**:
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true              # ✅ 启用 Redis
      core:
        enable-locking: true       # ✅ 启用分布式锁
    scheduling:
      enabled: false               # Redis 自动过期

spring:
  data:
    redis:
      host: redis.example.com
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

**适用条件**:
- 多个应用实例(负载均衡)
- 用户量 > 1000
- 并发请求 > 100 QPS
- 需要持久化和高可用

---

#### 场景四: 大规模分布式环境(高并发)

**推荐**: Redis Cluster 集群

**配置**:
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true

spring:
  data:
    redis:
      cluster:
        nodes:
          - redis1.example.com:6379
          - redis2.example.com:6379
          - redis3.example.com:6379
          - redis4.example.com:6379
          - redis5.example.com:6379
          - redis6.example.com:6379
        max-redirects: 3
      lettuce:
        pool:
          max-active: 50
          max-idle: 30
          min-idle: 10
```

**适用条件**:
- 用户量 > 10000
- 并发请求 > 1000 QPS
- 需要水平扩展能力
- 需要 99.99% 高可用

---

### 3.3 存储切换配置示例

#### 从内存切换到 Redis

**步骤 1**: 添加 Redis 依赖(如果尚未添加)

```xml
<dependency>
    <groupId>com.imaping</groupId>
    <artifactId>imaping-token-redis-registry</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>
```

**步骤 2**: 修改配置文件

```yaml
# 从这个配置
imaping:
  token:
    registry:
      redis:
        enabled: false  # ❌ 内存存储

# 改为这个配置
imaping:
  token:
    registry:
      redis:
        enabled: true   # ✅ Redis 存储

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**步骤 3**: 重启应用

⚠️ **注意**: 切换存储后,现有 Token 会失效,用户需重新登录。

---

### 3.4 性能和资源消耗对比

#### 内存占用估算

**内存存储**:
- 每个 Token 约 500 字节(包括 Token 对象和 ConcurrentHashMap 开销)
- 10000 个 Token ≈ 5 MB
- 100000 个 Token ≈ 50 MB

**Redis 存储**:
- 每个 Token 约 300 字节(序列化后)
- 10000 个 Token ≈ 3 MB
- 100000 个 Token ≈ 30 MB

#### 性能基准(参考值)

| 操作 | 内存存储 | Redis 单机 | Redis 集群 |
|------|---------|-----------|-----------|
| **创建 Token** | < 1 ms | 2-5 ms | 3-8 ms |
| **查询 Token** | < 0.1 ms | 1-3 ms | 2-5 ms |
| **删除 Token** | < 0.1 ms | 1-3 ms | 2-5 ms |
| **吞吐量(QPS)** | 50000+ | 10000+ | 20000+ |

**说明**: 实际性能受服务器配置、网络延迟、并发数等因素影响。

---

## 4. 多实例部署配置

### 4.1 负载均衡配置(Nginx 示例)

#### Nginx 配置文件

```nginx
# /etc/nginx/conf.d/imaping-token.conf

# 上游服务器组(多个应用实例)
upstream imaping_token_backend {
    # 负载均衡策略: ip_hash 确保同一客户端请求发送到同一服务器
    ip_hash;

    # 应用实例列表
    server app1.example.com:8080 max_fails=3 fail_timeout=30s;
    server app2.example.com:8080 max_fails=3 fail_timeout=30s;
    server app3.example.com:8080 max_fails=3 fail_timeout=30s;

    # 备用服务器(可选)
    # server app4.example.com:8080 backup;
}

server {
    listen 80;
    listen 443 ssl http2;
    server_name api.example.com;

    # SSL 证书配置(生产环境必须)
    ssl_certificate /etc/nginx/ssl/api.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 日志配置
    access_log /var/log/nginx/imaping-token-access.log;
    error_log /var/log/nginx/imaping-token-error.log;

    # 反向代理配置
    location / {
        proxy_pass http://imaping_token_backend;

        # 传递真实客户端 IP
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Token 相关 Header 透传
        proxy_set_header Authorization $http_authorization;
        proxy_set_header Cookie $http_cookie;

        # 超时配置
        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        # 错误处理
        proxy_next_upstream error timeout http_502 http_503 http_504;
    }

    # 健康检查端点(不走负载均衡)
    location /actuator/health {
        proxy_pass http://imaping_token_backend;
        access_log off;
    }

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        proxy_pass http://imaping_token_backend;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

**负载均衡策略对比**:

| 策略 | 说明 | 适用场景 | 配置 |
|------|------|---------|------|
| **round-robin** | 轮询(默认) | 服务器性能相近 | `upstream backend { server ...; }` |
| **ip_hash** | 基于客户端 IP 哈希 | 需要会话保持(使用 Cookie 时) | `ip_hash;` |
| **least_conn** | 最少连接数 | 服务器性能差异较大 | `least_conn;` |
| **weight** | 加权轮询 | 服务器性能差异较大 | `server ... weight=3;` |

**推荐配置**:
- 使用 Redis 存储时,使用 `round-robin` 或 `least_conn`
- 使用内存存储时,必须使用 `ip_hash` 保持会话

---

### 4.2 Redis 共享会话配置

多实例部署时,**必须使用 Redis 存储**实现会话共享。

#### 应用实例配置(所有实例相同)

```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true              # ✅ 启用 Redis 共享存储
      core:
        enable-locking: true       # ✅ 启用分布式锁(多实例必须)
    scheduling:
      enabled: false               # Redis 自动过期,无需定时清理

spring:
  data:
    redis:
      # 所有实例连接同一个 Redis
      host: redis.example.com      # 统一 Redis 服务器
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

**关键配置说明**:
- **`enable-locking: true`**: 多实例部署**必须启用**,防止并发冲突
- **相同 Redis 配置**: 所有实例必须连接到同一个 Redis 服务器或集群
- **相同 database 索引**: 确保所有实例使用同一个 Redis database

---

### 4.3 分布式锁配置说明

#### 为什么需要分布式锁?

多实例场景下,可能出现以下并发冲突:
1. **并发删除**: 多个实例同时删除同一个 Token
2. **并发更新**: 自动续期 Token 时,多个实例同时更新 `lastTimeUsed`
3. **并发清理**: 多个实例同时执行过期 Token 清理

#### 分布式锁配置

```yaml
imaping:
  token:
    registry:
      core:
        enable-locking: true       # ✅ 启用分布式锁
        lock-timeout: 5000         # 锁超时时间(毫秒),默认 5000
        lock-retry-times: 3        # 锁获取重试次数,默认 3
```

**锁超时时间建议**:
- 低并发: 5000ms(默认)
- 中并发: 3000ms
- 高并发: 2000ms

⚠️ **注意**: 分布式锁会略微降低性能(约 5-10%),但多实例场景下必须启用。

---

### 4.4 健康检查配置

#### Spring Boot Actuator 健康检查

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info       # 暴露健康检查端点
  endpoint:
    health:
      show-details: when-authorized  # 授权后显示详细信息
```

#### 健康检查接口

```
GET /actuator/health
```

**响应示例**:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

#### Nginx 健康检查配置

```nginx
upstream imaping_token_backend {
    server app1.example.com:8080 max_fails=3 fail_timeout=30s;
    server app2.example.com:8080 max_fails=3 fail_timeout=30s;

    # 健康检查(需要 nginx-plus 或 nginx-healthcheck 模块)
    check interval=5000 rise=2 fall=3 timeout=3000 type=http;
    check_http_send "GET /actuator/health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_2xx http_3xx;
}
```

---

### 4.5 集群部署架构图(文字描述)

```
                         ┌─────────────────┐
                         │   Nginx LB      │
                         │  (负载均衡器)     │
                         └────────┬────────┘
                                  │
                 ┌────────────────┼────────────────┐
                 │                │                │
          ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
          │   App 实例1  │  │   App 实例2  │  │   App 实例3  │
          │  (8080端口)  │  │  (8080端口)  │  │  (8080端口)  │
          └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
                 │                │                │
                 └────────────────┼────────────────┘
                                  │
                         ┌────────▼────────┐
                         │  Redis 集群     │
                         │ (共享 Token 存储)│
                         └─────────────────┘

流程说明:
1. 客户端请求发送到 Nginx 负载均衡器
2. Nginx 根据负载均衡策略分发请求到某个应用实例
3. 应用实例从 Redis 集群读取/写入 Token
4. 所有实例共享 Redis 中的 Token 数据,实现会话共享
5. 某个实例宕机时,其他实例继续提供服务(高可用)
```

**关键特性**:
- ✅ 高可用: 单个实例宕机不影响服务
- ✅ 水平扩展: 可动态增加实例应对流量增长
- ✅ 会话共享: 用户请求可由任意实例处理
- ✅ 负载均衡: 请求均匀分布到各实例

---

## 5. Spring Boot Actuator 集成

### 5.1 添加 Actuator 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

### 5.2 暴露健康检查端点

#### 基础配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 暴露端点
      base-path: /actuator              # 端点基础路径(默认)
  endpoint:
    health:
      show-details: when-authorized     # 授权后显示详细信息
      # show-details: always            # 总是显示详细信息(开发环境)
```

#### 访问端点

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 指标信息
curl http://localhost:8080/actuator/metrics
```

---

### 5.3 自定义健康指标(Token 统计)

创建自定义健康指标,监控 Token 数量和状态:

```java
package com.example.actuator;

import com.imaping.token.api.registry.TokenRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Token 健康指标
 *
 * <p>监控 Token 数量,当 Token 数量超过阈值时报警</p>
 */
@Component
public class TokenHealthIndicator implements HealthIndicator {

    private final TokenRegistry tokenRegistry;

    private static final long WARNING_THRESHOLD = 10000;  // 警告阈值
    private static final long CRITICAL_THRESHOLD = 50000; // 严重阈值

    public TokenHealthIndicator(TokenRegistry tokenRegistry) {
        this.tokenRegistry = tokenRegistry;
    }

    @Override
    public Health health() {
        long tokenCount = tokenRegistry.sessionCount();

        if (tokenCount >= CRITICAL_THRESHOLD) {
            return Health.down()
                .withDetail("tokenCount", tokenCount)
                .withDetail("threshold", CRITICAL_THRESHOLD)
                .withDetail("message", "Token 数量超过严重阈值,可能存在内存泄漏")
                .build();
        } else if (tokenCount >= WARNING_THRESHOLD) {
            return Health.up()
                .withDetail("tokenCount", tokenCount)
                .withDetail("threshold", WARNING_THRESHOLD)
                .withDetail("message", "Token 数量接近警告阈值")
                .status("WARNING")
                .build();
        } else {
            return Health.up()
                .withDetail("tokenCount", tokenCount)
                .withDetail("message", "Token 数量正常")
                .build();
        }
    }
}
```

**健康检查响应示例**:

```json
{
  "status": "UP",
  "components": {
    "token": {
      "status": "UP",
      "details": {
        "tokenCount": 1523,
        "message": "Token 数量正常"
      }
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

---

### 5.4 Prometheus 指标集成(可选)

#### 添加 Micrometer Prometheus 依赖

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### 配置 Prometheus 端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 暴露 Prometheus 端点
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 访问 Prometheus 指标

```bash
curl http://localhost:8080/actuator/prometheus
```

#### 自定义 Token 指标

```java
package com.example.metrics;

import com.imaping.token.api.registry.TokenRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Token 指标注册器
 *
 * <p>将 Token 相关指标注册到 Micrometer,供 Prometheus 采集</p>
 */
@Component
public class TokenMetrics {

    private final TokenRegistry tokenRegistry;
    private final MeterRegistry meterRegistry;

    public TokenMetrics(TokenRegistry tokenRegistry, MeterRegistry meterRegistry) {
        this.tokenRegistry = tokenRegistry;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerMetrics() {
        // 注册 Token 数量指标
        Gauge.builder("imaping_token_count", tokenRegistry, TokenRegistry::sessionCount)
            .description("当前活跃 Token 数量")
            .register(meterRegistry);
    }
}
```

**Prometheus 采集配置** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'imaping-token'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['app1.example.com:8080', 'app2.example.com:8080']
```

---

### 5.5 监控端点安全配置

生产环境必须保护监控端点,防止未授权访问。

#### 方式一: 使用独立端口

```yaml
management:
  server:
    port: 9090               # 监控端点使用独立端口
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

**防火墙配置**: 仅允许监控系统访问 9090 端口。

#### 方式二: 使用 Spring Security 保护

```java
@Configuration
public class ActuatorSecurityConfig extends TokenSecurityConfig {

    @Override
    protected String[] getPermitAntMatchers() {
        return new String[]{
            "/actuator/health",     // 健康检查公开(用于负载均衡)
            "/login"
        };
    }

    @Override
    protected String[] getAuthenticatedAntMatchers() {
        return new String[]{
            "/actuator/**",         // 其他 Actuator 端点需要认证
            "/api/**"
        };
    }
}
```

---

## 6. 常见集成场景

### 6.1 场景: 前后端分离项目 + Redis 集群

**架构**: Vue.js 前端 + Spring Boot 后端(3 个实例) + Redis Cluster

#### 后端配置

```yaml
# application-prod.yml
imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true
    accessToken:
      timeToKillInSeconds: 7200
    scheduling:
      enabled: false

spring:
  data:
    redis:
      cluster:
        nodes:
          - redis1.prod.example.com:6379
          - redis2.prod.example.com:6379
          - redis3.prod.example.com:6379
          - redis4.prod.example.com:6379
          - redis5.prod.example.com:6379
          - redis6.prod.example.com:6379
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 30
          max-idle: 15
          min-idle: 10
```

#### Security 配置

```java
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig extends TokenSecurityConfig {

    @Override
    protected String[] getPermitAntMatchers() {
        return new String[]{"/api/v1/auth/**", "/actuator/health"};
    }

    @Override
    protected Map<HttpMethod, String[]> getAuthenticatedAntMatchersWithMethod() {
        Map<HttpMethod, String[]> map = new HashMap<>();
        map.put(HttpMethod.POST, new String[]{"/api/v1/**"});
        map.put(HttpMethod.PUT, new String[]{"/api/v1/**"});
        map.put(HttpMethod.DELETE, new String[]{"/api/v1/**"});
        return map;
    }

    @Override
    protected Map<HttpMethod, String[]> getPermitAntMatchersWithMethod() {
        Map<HttpMethod, String[]> map = new HashMap<>();
        map.put(HttpMethod.OPTIONS, new String[]{"/**"}); // CORS 预检
        return map;
    }
}
```

#### 前端集成(Axios)

```javascript
// api/client.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'https://api.example.com',
  timeout: 10000,
});

// 请求拦截器: 添加 Token
apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器: 处理 401 错误
apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // Token 过期或无效,跳转到登录页
      localStorage.removeItem('access_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

### 6.2 场景: 微服务架构 + API 网关

**架构**: Spring Cloud Gateway + 多个微服务 + Redis Sentinel

#### API 网关配置

```yaml
# API 网关不需要 imaping-token 依赖,仅转发 Token
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - PreserveHostHeader  # 保留原始 Host header
```

#### 微服务配置(相同配置)

```yaml
# 所有微服务使用相同配置
imaping:
  token:
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true

spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - sentinel1.example.com:26379
          - sentinel2.example.com:26379
          - sentinel3.example.com:26379
        password: ${SENTINEL_PASSWORD}
      password: ${REDIS_PASSWORD}
```

---

### 6.3 场景: 传统单体应用迁移

**需求**: 从基于 Session 的认证迁移到基于 Token 的认证

#### 迁移步骤

**步骤 1**: 添加 imaping-token 依赖

**步骤 2**: 创建兼容配置(同时支持 Session 和 Token)

```java
@Configuration
@EnableWebSecurity
public class HybridSecurityConfig {

    @Bean
    @Order(1)  // Token 认证优先
    public SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
        // Token 认证配置(参考前面示例)
        return http.build();
    }

    @Bean
    @Order(2)  // Session 认证备用
    public SecurityFilterChain sessionFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(registry ->
                registry.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .sessionManagement(configurer ->
                configurer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}
```

**步骤 3**: 逐步迁移客户端从 Session 切换到 Token

**步骤 4**: 完全移除 Session 配置

---

## 附录

### A. 配置项速查表

| 配置项 | 说明 | 默认值 | 推荐值(生产) |
|--------|------|--------|-------------|
| `imaping.token.accessTokenName` | Token 名称(Cookie/Parameter) | `access_token` | `access_token` |
| `imaping.token.registry.redis.enabled` | 启用 Redis 存储 | `false` | `true` |
| `imaping.token.registry.core.enable-locking` | 启用分布式锁 | `false` | `true`(多实例) |
| `imaping.token.accessToken.timeToKillInSeconds` | Token 过期时间(秒) | `7200` | `7200` |
| `imaping.token.scheduling.enabled` | 启用定时清理 | `false` | `false`(Redis)/ `true`(内存) |
| `spring.data.redis.lettuce.pool.max-active` | 最大连接数 | `8` | `20`(中并发) / `50`(高并发) |

### B. 相关文档链接

- [架构文档](architecture.md) - 系统设计和组件详解
- [配置参考](configuration.md) - 完整配置项列表
- [API 使用指南](api-guide.md) - API 使用示例
- [快速入门指南](quick-start.md) - 5 分钟快速体验

### C. 外部资源

- [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/)
- [Spring Data Redis 官方文档](https://docs.spring.io/spring-data/redis/reference/)
- [Redis 官方文档](https://redis.io/docs/)
- [Nginx 官方文档](https://nginx.org/en/docs/)

---

**维护责任**: 架构团队
**最后更新**: 2025-10-12
**文档版本**: v1.0
**反馈渠道**: docs@example.com
