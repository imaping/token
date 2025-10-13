# 最佳实践

> **快速参考**: imaping-token 系统生产环境最佳实践和优化建议
> **最后更新**: 2025-10-12
> **适用版本**: 0.0.2-SNAPSHOT

---

## 目录

1. [Token 过期策略选择指南](#1-token-过期策略选择指南)
2. [性能优化建议](#2-性能优化建议)
3. [安全性建议](#3-安全性建议)
4. [故障排查指南](#4-故障排查指南)
5. [监控和告警建议](#5-监控和告警建议)
6. [开发和测试最佳实践](#6-开发和测试最佳实践)

---

## 1. Token 过期策略选择指南

### 1.1 两种过期策略对比

imaping-token 提供两种 Token 过期策略,适用于不同场景:

| 策略类型 | 接口 | 实现类 | 行为特征 | 适用场景 |
|---------|------|--------|---------|---------|
| **自动续期** | `TimeoutAccessToken` | `DefaultTimeoutAccessToken` | 每次使用时更新 `lastTimeUsed`,滑动窗口过期 | • Web 会话管理<br>• 需要保持活跃的用户会话<br>• 桌面应用登录 |
| **固定时间** | `HardTimeoutToken` | `DefaultHardTimeoutToken` | 从创建时间开始计算,固定时间后过期 | • API Token<br>• 短期访问令牌<br>• 一次性令牌<br>• 敏感操作授权 |

---

### 1.2 自动续期策略(TimeoutAccessToken)

#### 工作原理

```
创建时间: 2025-01-01 10:00:00
TTL: 7200秒 (2小时)

10:00:00 - 创建 Token,过期时间 = 12:00:00
10:30:00 - 用户访问,更新 lastTimeUsed,过期时间 = 12:30:00  ← 自动续期
11:00:00 - 用户访问,更新 lastTimeUsed,过期时间 = 13:00:00  ← 自动续期
13:00:00 - 无访问超过 2 小时,Token 过期
```

#### 使用场景

**✅ 推荐使用**:
1. **Web 应用用户登录**: 用户持续操作时保持登录状态
2. **单页应用(SPA)**: 前端持续调用 API 时保持会话
3. **移动应用**: 用户活跃时保持登录,长时间不用自动登出
4. **管理后台**: 管理员操作时保持登录,离开一段时间后自动登出

**❌ 不推荐使用**:
1. **API Token**: 固定有效期更安全,避免无限续期
2. **短期授权码**: 需要明确的过期时间
3. **敏感操作**: 如支付确认、密码修改,应使用固定时间策略

#### 配置示例

```yaml
imaping:
  token:
    accessToken:
      timeToKillInSeconds: 7200  # 2 小时无活动后过期
```

#### 推荐过期时间

| 场景 | 推荐过期时间 | 说明 |
|------|------------|------|
| **Web 应用** | 1-2 小时 | 平衡用户体验和安全性 |
| **管理后台** | 30-60 分钟 | 更高的安全要求 |
| **移动应用** | 4-8 小时 | 移动场景下用户可能间歇使用 |

---

### 1.3 固定时间策略(HardTimeoutToken)

#### 工作原理

```
创建时间: 2025-01-01 10:00:00
TTL: 3600秒 (1小时)

10:00:00 - 创建 Token,过期时间 = 11:00:00
10:30:00 - 用户访问,过期时间仍为 11:00:00  ← 不续期
10:45:00 - 用户访问,过期时间仍为 11:00:00  ← 不续期
11:00:00 - Token 过期,无论是否使用
```

#### 使用场景

**✅ 推荐使用**:
1. **API Token**: 第三方应用访问令牌,固定有效期
2. **短期授权码**: 如邮箱验证码、短信验证码
3. **敏感操作**: 如支付确认、密码修改,需要重新认证
4. **一次性令牌**: 如密码重置链接、邀请码

**❌ 不推荐使用**:
1. **长期会话**: 用户可能在有效期内活跃,但被强制登出
2. **交互式应用**: 用户体验较差,频繁需要重新登录

#### 配置示例

自定义 `HardTimeoutToken` 工厂(参见 [API 使用指南](api-guide.md#43-自定义过期策略)):

```java
@Bean
public TokenFactory hardTimeoutTokenFactory(
        UniqueTokenIdGenerator tokenIdGenerator,
        HardTimeoutExpirationPolicy expirationPolicy) {
    return new HardTimeoutTokenDefaultFactory(tokenIdGenerator, expirationPolicy);
}
```

#### 推荐过期时间

| 场景 | 推荐过期时间 | 说明 |
|------|------------|------|
| **API Token** | 1-7 天 | 长期访问令牌,配合 Refresh Token 使用 |
| **短期访问令牌** | 5-15 分钟 | 临时授权,如 OAuth2 Access Token |
| **验证码** | 5-10 分钟 | 邮箱/短信验证码 |
| **敏感操作** | 5-15 分钟 | 支付确认、密码修改等 |

---

### 1.4 过期策略选择决策树

```
开始
  │
  ├─ 是否需要用户长期保持登录状态?
  │   ├─ 是 → 用户是否持续活跃时需要保持会话?
  │   │         ├─ 是 → 使用 TimeoutAccessToken (自动续期)
  │   │         └─ 否 → 使用 HardTimeoutToken (固定时间)
  │   └─ 否 → 是否为敏感操作或临时授权?
  │             ├─ 是 → 使用 HardTimeoutToken (固定时间)
  │             └─ 否 → 使用 TimeoutAccessToken (自动续期)
```

---

### 1.5 自定义过期策略示例

#### 场景: 白天工作时间自动续期,晚上强制过期

```java
package com.example.expiration;

import com.imaping.token.api.expiration.AbstractTokenExpirationPolicy;
import com.imaping.token.api.model.Token;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作时间自动续期策略
 *
 * <p>白天 9:00-18:00 自动续期,晚上强制过期</p>
 */
public class WorkHoursExpirationPolicy extends AbstractTokenExpirationPolicy {

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    private final long timeToKillInSeconds;

    public WorkHoursExpirationPolicy(long timeToKillInSeconds) {
        this.timeToKillInSeconds = timeToKillInSeconds;
    }

    @Override
    public boolean isExpired(Token token) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        // 非工作时间,立即过期
        if (currentTime.isBefore(WORK_START) || currentTime.isAfter(WORK_END)) {
            return true;
        }

        // 工作时间内,按自动续期策略
        long lastUsedTime = token.getLastTimeUsed();
        long currentTimeMillis = System.currentTimeMillis();
        return (currentTimeMillis - lastUsedTime) > (timeToKillInSeconds * 1000);
    }
}
```

---

## 2. 性能优化建议

### 2.1 缓存策略选择

#### Caffeine 缓存配置(内存存储)

Caffeine 是高性能的本地缓存库,可显著提升 Token 查询性能。

```java
package com.example.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.imaping.token.api.registry.CachingTokenRegistry;
import com.imaping.token.api.registry.TokenRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Token 缓存配置
 */
@Configuration
public class TokenCacheConfig {

    /**
     * 配置带 Caffeine 缓存的 TokenRegistry
     *
     * <p>缓存配置说明:</p>
     * <ul>
     *   <li>maximumSize: 最大缓存 Token 数量,超过后按 LFU 算法淘汰</li>
     *   <li>expireAfterWrite: 写入后 2 小时过期(与 Token TTL 一致)</li>
     *   <li>expireAfterAccess: 访问后 1 小时内不淘汰(可选)</li>
     *   <li>recordStats: 记录缓存统计信息,用于监控</li>
     * </ul>
     */
    @Bean
    public TokenRegistry cachingTokenRegistry() {
        return new CachingTokenRegistry(
            Caffeine.newBuilder()
                .maximumSize(10000)                      // 最大缓存 10000 个 Token
                .expireAfterWrite(Duration.ofHours(2))   // 写入 2 小时后过期
                .expireAfterAccess(Duration.ofHours(1))  // 访问 1 小时后过期(可选)
                .recordStats()                           // 记录统计信息
                .build()
        );
    }
}
```

#### 缓存参数调优建议

| 参数 | 说明 | 推荐值(低并发) | 推荐值(中并发) | 推荐值(高并发) |
|------|------|--------------|--------------|--------------|
| **maximumSize** | 最大缓存数量 | 1000 | 10000 | 50000+ |
| **expireAfterWrite** | 写入后过期时间 | 与 Token TTL 一致 | 与 Token TTL 一致 | 与 Token TTL 一致 |
| **expireAfterAccess** | 访问后过期时间 | TTL * 0.5 | TTL * 0.5 | TTL * 0.3 |

**说明**:
- `maximumSize` 应略大于预期同时在线用户数
- `expireAfterWrite` 应与 Token TTL 一致,避免过期 Token 被缓存
- `expireAfterAccess` 可选,用于提前淘汰不活跃 Token

---

### 2.2 Redis 性能优化

#### Pipeline 批量操作

批量操作时使用 Redis Pipeline 减少网络往返:

```java
// ❌ 低效: 逐个删除(N 次网络往返)
for (String tokenId : tokenIds) {
    tokenRegistry.deleteToken(tokenId);
}

// ✅ 高效: 批量删除(1 次网络往返)
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (String tokenId : tokenIds) {
        connection.del(tokenId.getBytes());
    }
    return null;
});
```

#### 连接池优化

根据并发量调整 Lettuce 连接池参数:

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20           # 峰值并发数 * 1.5
          max-idle: 10             # max-active 的 50%
          min-idle: 5              # 保持一定空闲连接
          max-wait: 2000ms         # 最大等待时间
          time-between-eviction-runs: 60000ms  # 空闲连接检查间隔
```

#### 序列化优化

使用高效的序列化方式减少网络传输:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    // 使用 Jackson2JsonRedisSerializer (推荐,可读性好)
    Jackson2JsonRedisSerializer<Object> serializer =
        new Jackson2JsonRedisSerializer<>(Object.class);

    // 或使用 GenericJackson2JsonRedisSerializer (支持多态)
    // GenericJackson2JsonRedisSerializer serializer =
    //     new GenericJackson2JsonRedisSerializer();

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    return template;
}
```

---

### 2.3 Token ID 长度优化建议

Token ID 长度直接影响安全性、性能和存储开销。

#### 安全性 vs 性能权衡

| Token 长度 | 熵值(位) | 安全性 | 性能 | 存储开销 | 推荐场景 |
|-----------|---------|-------|------|---------|---------|
| **16 字符** | 128 位 | ⚠️ 中等 | ⚡ 很快 | 很小 | 内部系统、低风险应用 |
| **32 字符** | 256 位 | ✅ 高 | ⚡ 快 | 小 | **推荐:生产环境** |
| **64 字符** | 512 位 | ✅ 极高 | 较快 | 中 | 极高安全要求场景 |

#### 推荐配置

**生产环境(推荐)**:
```java
@Bean
public UniqueTokenIdGenerator tokenIdGenerator() {
    return new DefaultUniqueTokenIdGenerator(
        new Base64RandomStringGenerator(),
        32  // ✅ 32 字符 = 256 位熵,安全且高效
    );
}
```

**高安全场景**:
```java
@Bean
public UniqueTokenIdGenerator tokenIdGenerator() {
    return new DefaultUniqueTokenIdGenerator(
        new Base64RandomStringGenerator(),
        64  // ✅ 64 字符 = 512 位熵,适用于金融、医疗等高安全场景
    );
}
```

**说明**:
- ✅ **推荐 32 字符**: 256 位熵足以抵御暴力破解,性能和安全性平衡
- ❌ **不推荐 < 16 字符**: 安全性不足,容易被暴力破解
- ⚠️ **慎用 > 64 字符**: 性能和存储开销增加,安全性提升有限

---

### 2.4 批量操作最佳实践

#### 批量删除 Token

```java
/**
 * 批量删除过期 Token
 *
 * @param tokenIds Token ID 列表
 */
public void batchDeleteTokens(List<String> tokenIds) {
    if (tokenIds == null || tokenIds.isEmpty()) {
        return;
    }

    // 分批处理,避免一次性处理过多
    int batchSize = 100;
    for (int i = 0; i < tokenIds.size(); i += batchSize) {
        int end = Math.min(i + batchSize, tokenIds.size());
        List<String> batch = tokenIds.subList(i, end);

        // 使用 Pipeline 批量删除
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String tokenId : batch) {
                tokenRegistry.deleteToken(tokenId);
            }
            return null;
        });
    }
}
```

---

### 2.5 JVM 参数优化建议

#### 堆内存配置

```bash
# 生产环境推荐配置
java -Xmx2g \                      # 最大堆内存 2GB
     -Xms2g \                      # 初始堆内存 2GB (与 -Xmx 相同避免动态扩展)
     -XX:+UseG1GC \                # 使用 G1 垃圾收集器
     -XX:MaxGCPauseMillis=200 \    # 最大 GC 暂停时间 200ms
     -XX:+HeapDumpOnOutOfMemoryError \  # OOM 时生成堆转储
     -XX:HeapDumpPath=/var/log/app/heap_dump.hprof \
     -jar app.jar
```

#### 堆内存大小建议

| 用户规模 | 推荐堆内存 | 说明 |
|---------|-----------|------|
| **< 1000 用户** | 512MB - 1GB | 小规模应用 |
| **1000-10000 用户** | 1GB - 2GB | 中等规模应用 |
| **> 10000 用户** | 2GB - 4GB+ | 大规模应用 |

**内存占用估算**:
- 每个 Token 约 500 字节(内存存储)
- 10000 个 Token ≈ 5 MB
- 预留 2-3 倍内存用于对象开销和 GC

---

## 3. 安全性建议

### 3.1 Token 长度和强度要求

#### 最低安全要求

| 安全级别 | Token 长度 | 熵值 | 适用场景 |
|---------|-----------|------|---------|
| **低** | 16 字符 | 128 位 | 内部系统、开发环境 |
| **中** | 24 字符 | 192 位 | 一般生产环境 |
| **高** | 32 字符 | 256 位 | **推荐:生产环境** |
| **极高** | 64 字符 | 512 位 | 金融、医疗等高安全场景 |

#### 推荐配置

```java
@Bean
public UniqueTokenIdGenerator tokenIdGenerator() {
    // ✅ 使用安全的随机生成器
    return new DefaultUniqueTokenIdGenerator(
        new Base64RandomStringGenerator(),  // 使用 SecureRandom
        32  // 256 位熵
    );
}

// ❌ 避免使用弱随机生成器
// new Random().nextInt()  // 不安全,可预测
// UUID.randomUUID()       // 仅 122 位熵,不足够安全
```

---

### 3.2 HTTPS 传输配置(SSL/TLS)

⚠️ **生产环境必须强制使用 HTTPS**,否则 Token 可能被中间人攻击截获。

#### Spring Boot HTTPS 配置

**方式一: 使用内嵌服务器 SSL**

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
```

**方式二: 使用反向代理(推荐)**

Nginx 处理 SSL/TLS,应用服务器使用 HTTP:

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/api.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;

    # SSL 协议和加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers on;

    # HSTS (强制 HTTPS)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$server_name$request_uri;
}
```

---

### 3.3 Cookie 安全属性

#### 推荐配置

```java
@PostMapping("/login")
public ResponseEntity<Void> login(HttpServletResponse response, @RequestBody LoginRequest request) {
    // ... 验证用户名密码,创建 Token ...

    Cookie cookie = new Cookie("access_token", token.getId());

    // ✅ 必须配置的安全属性
    cookie.setHttpOnly(true);    // 防止 XSS 攻击(JavaScript 无法访问)
    cookie.setSecure(true);       // 仅 HTTPS 传输(生产环境必须)
    cookie.setPath("/");          // Cookie 作用范围

    // ✅ 推荐配置的安全属性
    cookie.setMaxAge(7200);       // 过期时间(2 小时)
    // cookie.setAttribute("SameSite", "Strict"); // 防止 CSRF 攻击(Spring Boot 2.6+)

    response.addCookie(cookie);
    return ResponseEntity.ok().build();
}
```

#### Cookie 属性说明

| 属性 | 说明 | 推荐值 | 安全性影响 |
|------|------|-------|-----------|
| **HttpOnly** | JavaScript 无法访问 | `true` | 防止 XSS 攻击窃取 Token |
| **Secure** | 仅 HTTPS 传输 | `true`(生产) | 防止中间人攻击截获 Token |
| **SameSite** | 跨站请求限制 | `Strict`/`Lax` | 防止 CSRF 攻击 |
| **Path** | Cookie 作用路径 | `/` 或 `/api` | 限制 Cookie 发送范围 |
| **MaxAge** | Cookie 过期时间 | 与 Token TTL 一致 | 防止过期 Cookie 被使用 |

#### SameSite 属性选择

| 值 | 说明 | 推荐场景 |
|----|------|---------|
| **Strict** | 跨站请求完全禁止发送 Cookie | 高安全要求,纯 API 场景 |
| **Lax** | 顶级导航(如链接跳转)允许发送 Cookie | **推荐:大多数 Web 应用** |
| **None** | 跨站请求允许发送 Cookie(需 Secure=true) | 跨域嵌入场景(如 iframe) |

---

### 3.4 CSRF 保护配置

#### 场景一: 前后端分离(使用 Token 认证)

前后端分离项目通常**不需要 CSRF 保护**,因为:
- Token 存储在 `Authorization` Header,不会被浏览器自动发送
- 攻击者无法获取 Token

```java
@Configuration
public class SecurityConfig extends TokenSecurityConfig {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);  // ✅ 禁用 CSRF
    }
}
```

#### 场景二: 传统 Web 应用(使用 Cookie 认证)

使用 Cookie 存储 Token 时,**必须启用 CSRF 保护**:

```java
@Configuration
public class SecurityConfig extends TokenSecurityConfig {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        );
    }
}
```

**前端集成**:

```javascript
// 从 Cookie 读取 CSRF Token
function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? match[1] : null;
}

// Axios 配置
axios.defaults.headers.common['X-XSRF-TOKEN'] = getCsrfToken();
```

---

### 3.5 敏感信息日志脱敏

⚠️ **绝对不要在日志中记录完整 Token 或密码**

#### 推荐做法

```java
// ❌ 错误: 记录完整 Token
log.info("User login successful, token: {}", token.getId());

// ✅ 正确: 仅记录 Token 前 6 位
log.info("User login successful, token: {}...", token.getId().substring(0, 6));

// ✅ 正确: 使用脱敏工具类
log.info("User login successful, token: {}", maskToken(token.getId()));

/**
 * Token 脱敏
 *
 * @param token 原始 Token
 * @return 脱敏后的 Token(仅显示前 6 位)
 */
private String maskToken(String token) {
    if (token == null || token.length() <= 6) {
        return "***";
    }
    return token.substring(0, 6) + "..." + "(" + token.length() + " chars)";
}
```

#### 常见敏感信息脱敏

| 信息类型 | 脱敏方式 | 示例 |
|---------|---------|------|
| **Token** | 仅显示前 6 位 | `4xY7k9...` (32 chars) |
| **密码** | 完全隐藏 | `******` |
| **手机号** | 中间 4 位隐藏 | `138****5678` |
| **邮箱** | 部分隐藏 | `us***@example.com` |
| **身份证** | 中间隐藏 | `320***********1234` |

---

## 4. 故障排查指南

### 4.1 Token 认证失败(401 错误)

#### 现象

客户端收到 `401 Unauthorized` 响应,提示认证失败。

#### 可能原因和解决方案

| 原因 | 检查方法 | 解决方案 |
|------|---------|---------|
| **Token 不存在** | 日志: `Token not found: xxx` | 用户需重新登录获取 Token |
| **Token 已过期** | 检查 Token TTL 配置 | 调整 `timeToKillInSeconds` 或重新登录 |
| **Token 格式错误** | 检查 Header 格式:`Authorization: Bearer <token>` | 确保 Token 格式正确 |
| **Token 名称不匹配** | 检查 `accessTokenName` 配置 | 确保前后端 Token 名称一致 |
| **Redis 连接失败** | 日志: `Unable to connect to Redis` | 检查 Redis 配置和网络连接 |

#### 排查步骤

**步骤 1**: 检查请求中是否包含 Token

```bash
# 使用 curl 测试
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     http://localhost:8080/api/users

# 检查响应状态码
# 401 - Token 无效或不存在
# 200 - Token 有效
```

**步骤 2**: 检查 Token 是否存在于 TokenRegistry

```java
// 在控制器中添加调试代码
Token token = tokenRegistry.getToken(tokenId);
if (token == null) {
    log.error("Token not found in registry: {}", tokenId);
} else {
    log.info("Token found, user: {}", token.getAuthentication().getName());
}
```

**步骤 3**: 检查日志

```
# 查看应用日志
tail -f /var/log/app/application.log | grep "Token"

# 常见日志提示:
# - "Token not found" - Token 不存在或已删除
# - "Token expired" - Token 已过期
# - "Unable to connect to Redis" - Redis 连接失败
```

---

### 4.2 Token 过期处理

#### 现象

用户正常使用时突然被登出,提示 Token 过期。

#### 可能原因

| 原因 | 说明 | 解决方案 |
|------|------|---------|
| **TTL 设置过短** | `timeToKillInSeconds` 太小 | 增加过期时间(推荐 7200 秒) |
| **服务器时区不一致** | 多个实例时区不同 | 统一服务器时区为 UTC |
| **Redis TTL 未正确设置** | Redis Key 未设置 TTL | 检查 `RedisTokenRegistry` 实现 |
| **时钟漂移** | 服务器时钟不同步 | 使用 NTP 同步时钟 |

#### 排查步骤

**步骤 1**: 检查 Token 过期时间配置

```yaml
imaping:
  token:
    accessToken:
      timeToKillInSeconds: 7200  # 检查此配置
```

**步骤 2**: 检查服务器时区

```bash
# 查看服务器时区
date
timedatectl

# 统一设置为 UTC
sudo timedatectl set-timezone UTC
```

**步骤 3**: 检查 Redis TTL

```bash
# 连接 Redis
redis-cli

# 查看 Token 剩余过期时间(秒)
TTL "imaping.token:4xY7k9zP2mQ1nR3s:user12345"

# 输出:
# -2: Key 不存在
# -1: Key 存在但无过期时间(异常,应设置 TTL)
# > 0: 剩余过期时间(秒)
```

---

### 4.3 Redis 连接失败

#### 现象

应用启动失败或运行时抛出 Redis 连接异常:
```
Unable to connect to Redis; nested exception is
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

#### 可能原因和解决方案

| 原因 | 检查方法 | 解决方案 |
|------|---------|---------|
| **Redis 未启动** | `redis-cli ping` | 启动 Redis: `redis-server` |
| **端口配置错误** | 检查 `spring.data.redis.port` | 修改为正确端口(默认 6379) |
| **密码错误** | 尝试手动连接: `redis-cli -a password` | 检查 `spring.data.redis.password` |
| **网络不通** | `telnet redis-host 6379` | 检查防火墙和网络配置 |
| **连接池耗尽** | 查看日志: `Pool exhausted` | 增加 `max-active` 配置 |

#### 排查步骤

**步骤 1**: 检查 Redis 服务状态

```bash
# 检查 Redis 是否运行
redis-cli ping
# 输出 "PONG" 表示正常

# 检查 Redis 端口是否监听
netstat -tulpn | grep 6379

# 测试连接
telnet localhost 6379
```

**步骤 2**: 检查应用配置

```yaml
spring:
  data:
    redis:
      host: localhost       # 检查主机地址
      port: 6379            # 检查端口
      password:             # 检查密码
      database: 0           # 检查数据库索引
```

**步骤 3**: 检查防火墙规则

```bash
# 检查防火墙状态(CentOS)
sudo firewall-cmd --list-all

# 允许 Redis 端口
sudo firewall-cmd --add-port=6379/tcp --permanent
sudo firewall-cmd --reload
```

---

### 4.4 自动配置未生效问题

#### 现象

应用启动后 Token 认证不工作,或 Bean 未注入。

#### 可能原因和解决方案

| 原因 | 检查方法 | 解决方案 |
|------|---------|---------|
| **配置条件不满足** | 检查 `@ConditionalOnProperty` | 确保配置项正确 |
| **Bean 冲突** | 查看自动配置报告 | 移除冲突的 Bean 定义 |
| **依赖缺失** | 检查 `pom.xml` | 添加缺失的依赖 |
| **包扫描路径不正确** | 检查 `@ComponentScan` | 确保扫描到 imaping 包 |

#### 排查步骤

**步骤 1**: 启用调试模式查看自动配置报告

```bash
# 启动应用时添加 --debug 参数
java -jar app.jar --debug

# 或在配置文件中启用
debug: true
```

**查看输出**:
```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
TokenApiConfig matched:
  - @ConditionalOnProperty (imaping.token.enabled) matched

Negative matches:
-----------------
RedisTokenRegistry did not match:
  - @ConditionalOnProperty (imaping.token.registry.redis.enabled=false) did not match
```

**步骤 2**: 检查配置项

```yaml
imaping:
  token:
    enabled: true  # 确保启用
    registry:
      redis:
        enabled: true  # 如需 Redis,确保启用
```

**步骤 3**: 检查 Bean 是否注入

```java
@RestController
public class DebugController {

    @Autowired(required = false)
    private TokenRegistry tokenRegistry;

    @GetMapping("/debug/beans")
    public String checkBeans() {
        if (tokenRegistry == null) {
            return "TokenRegistry bean not found!";
        }
        return "TokenRegistry: " + tokenRegistry.getClass().getName();
    }
}
```

---

### 4.5 常见配置错误和解决方案

| 错误配置 | 症状 | 正确配置 |
|---------|------|---------|
| `imaping.token.registry.redis.enabled=true` 但未添加 Redis 依赖 | 启动失败:`ClassNotFoundException` | 添加 `imaping-token-redis-registry` 依赖 |
| `timeToKillInSeconds: -1` | Token 永不过期(内存泄漏) | 设置合理值如 `7200` |
| `enable-locking: false` (多实例) | 并发冲突,Token 状态不一致 | 多实例必须设置 `enable-locking: true` |
| 不同实例连接不同 Redis | Token 无法共享,用户重复登录 | 所有实例连接同一个 Redis |
| Cookie `SameSite=None` 但 `Secure=false` | Cookie 不被发送 | `SameSite=None` 必须配合 `Secure=true` |

---

## 5. 监控和告警建议

### 5.1 关键指标监控

#### Token 数量监控

使用 Actuator 自定义指标监控活跃 Token 数量:

```java
package com.example.metrics;

import com.imaping.token.api.registry.TokenRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

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
        // Token 数量指标
        Gauge.builder("imaping_token_active_count", tokenRegistry, TokenRegistry::sessionCount)
            .description("当前活跃 Token 数量")
            .register(meterRegistry);

        // 可以添加更多指标...
    }
}
```

**访问指标**:
```bash
curl http://localhost:8080/actuator/metrics/imaping_token_active_count
```

**响应示例**:
```json
{
  "name": "imaping_token_active_count",
  "measurements": [
    {"statistic": "VALUE", "value": 1523}
  ]
}
```

---

#### Token 过期率监控

```java
@Component
public class TokenExpirationMetrics {

    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong expiredTokens = new AtomicLong(0);

    public TokenExpirationMetrics(MeterRegistry registry) {
        Gauge.builder("imaping_token_total", totalTokens, AtomicLong::get)
            .description("累计创建 Token 数量")
            .register(registry);

        Gauge.builder("imaping_token_expired", expiredTokens, AtomicLong::get)
            .description("累计过期 Token 数量")
            .register(registry);

        Gauge.builder("imaping_token_expiration_rate", this, metrics -> {
            long total = metrics.totalTokens.get();
            long expired = metrics.expiredTokens.get();
            return total > 0 ? (double) expired / total * 100 : 0;
        })
            .description("Token 过期率(%)")
            .register(registry);
    }

    public void recordTokenCreated() {
        totalTokens.incrementAndGet();
    }

    public void recordTokenExpired() {
        expiredTokens.incrementAndGet();
    }
}
```

---

### 5.2 Redis 监控指标

#### 连接池监控

```yaml
management:
  metrics:
    enable:
      lettuce: true  # 启用 Lettuce 连接池指标
```

**关键指标**:
- `lettuce.pool.active`: 活跃连接数
- `lettuce.pool.idle`: 空闲连接数
- `lettuce.pool.pending`: 等待连接数

**访问指标**:
```bash
curl http://localhost:8080/actuator/metrics/lettuce.pool.active
```

---

#### Redis 性能监控

使用 Redis 自带的 `INFO` 命令监控:

```bash
# 连接 Redis
redis-cli

# 查看统计信息
INFO stats

# 关键指标:
# - total_commands_processed: 处理的命令总数
# - instantaneous_ops_per_sec: 当前 QPS
# - used_memory: 内存使用量
# - connected_clients: 连接的客户端数
```

---

### 5.3 告警规则建议

#### Prometheus 告警规则示例

```yaml
# prometheus_alerts.yml
groups:
  - name: imaping_token_alerts
    interval: 1m
    rules:
      # Token 数量异常告警
      - alert: TokenCountTooHigh
        expr: imaping_token_active_count > 50000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Token 数量过高"
          description: "当前活跃 Token 数量 {{ $value }},超过 50000 阈值"

      # Token 过期率异常告警
      - alert: TokenExpirationRateHigh
        expr: imaping_token_expiration_rate > 80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Token 过期率过高"
          description: "Token 过期率 {{ $value }}%,可能存在配置问题"

      # Redis 连接失败告警
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis 连接失败"
          description: "Redis 服务不可用,Token 存储失败"

      # 连接池耗尽告警
      - alert: RedisPoolExhausted
        expr: lettuce_pool_pending > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Redis 连接池耗尽"
          description: "等待连接数 {{ $value }},需增加 max-active 配置"
```

---

### 5.4 日志记录最佳实践

#### 日志级别使用

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 系统错误、异常 | `log.error("Failed to create token", e)` |
| **WARN** | 警告、潜在问题 | `log.warn("Token count exceeds threshold: {}", count)` |
| **INFO** | 重要业务事件 | `log.info("User {} logged in successfully", username)` |
| **DEBUG** | 调试信息 | `log.debug("Token {} retrieved from cache", tokenId)` |
| **TRACE** | 详细跟踪信息 | `log.trace("Token details: {}", token)` |

#### 推荐日志配置

```yaml
# application.yml
logging:
  level:
    com.imaping.token: INFO                       # Token 组件日志级别
    com.imaping.token.api.registry: DEBUG         # Registry 详细日志
    org.springframework.security: INFO            # Security 日志
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/app/application.log
    max-size: 100MB
    max-history: 30
```

---

### 5.5 Grafana 监控仪表盘(推荐面板)

#### 推荐监控面板

1. **Token 数量趋势图** (折线图)
   - 指标: `imaping_token_active_count`
   - 时间范围: 最近 24 小时

2. **Token 过期率** (仪表盘)
   - 指标: `imaping_token_expiration_rate`
   - 阈值: > 80% 警告, > 90% 严重

3. **Redis 连接池状态** (柱状图)
   - 指标: `lettuce.pool.active`, `lettuce.pool.idle`, `lettuce.pool.pending`

4. **JVM 内存使用** (折线图)
   - 指标: `jvm.memory.used`, `jvm.memory.max`

5. **响应时间分布** (热力图)
   - 指标: `http.server.requests.duration`

---

## 6. 开发和测试最佳实践

### 6.1 单元测试建议

#### 测试 Token 创建和验证

```java
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRegistry tokenRegistry;

    @InjectMocks
    private TokenService tokenService;

    @Test
    @DisplayName("应该成功创建 Token")
    void shouldCreateTokenSuccessfully() {
        // Given
        Authentication auth = new Authentication("testuser", "password");
        Token expectedToken = new DefaultTimeoutAccessToken("token123", auth);
        when(tokenRegistry.addToken(any())).thenReturn(expectedToken);

        // When
        Token actualToken = tokenService.createToken(auth);

        // Then
        assertThat(actualToken).isNotNull();
        assertThat(actualToken.getId()).isEqualTo("token123");
        verify(tokenRegistry, times(1)).addToken(any());
    }

    @Test
    @DisplayName("应该正确验证 Token 有效性")
    void shouldValidateTokenCorrectly() {
        // Given
        String tokenId = "token123";
        Token token = new DefaultTimeoutAccessToken(tokenId, new Authentication("testuser", null));
        when(tokenRegistry.getToken(tokenId)).thenReturn(token);

        // When
        boolean isValid = tokenService.validateToken(tokenId);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Token 过期时应返回 false")
    void shouldReturnFalseWhenTokenExpired() {
        // Given
        String tokenId = "expired-token";
        when(tokenRegistry.getToken(tokenId)).thenReturn(null);

        // When
        boolean isValid = tokenService.validateToken(tokenId);

        // Then
        assertThat(isValid).isFalse();
    }
}
```

---

### 6.2 集成测试建议

#### 测试 Token 认证流程

```java
@SpringBootTest
@AutoConfigureMockMvc
class TokenAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenFactory tokenFactory;

    @Autowired
    private TokenRegistry tokenRegistry;

    @Test
    @DisplayName("未提供 Token 时应返回 401")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("提供有效 Token 时应返回 200")
    void shouldReturn200WithValidToken() throws Exception {
        // 创建 Token
        Authentication auth = new Authentication("testuser", null);
        Token token = tokenFactory.createToken(auth);

        // 使用 Token 访问
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token.getId()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token 过期后应返回 401")
    void shouldReturn401WhenTokenExpired() throws Exception {
        // 创建 Token
        Authentication auth = new Authentication("testuser", null);
        Token token = tokenFactory.createToken(auth);

        // 删除 Token 模拟过期
        tokenRegistry.deleteToken(token.getId());

        // 使用过期 Token
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token.getId()))
            .andExpect(status().isUnauthorized());
    }
}
```

---

### 6.3 性能测试建议

#### JMeter 测试计划示例

**测试场景 1: Token 创建性能**

```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>  <!-- 100 并发用户 -->
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>     <!-- 10 秒启动 -->
  <stringProp name="ThreadGroup.duration">60</stringProp>      <!-- 持续 60 秒 -->
</ThreadGroup>
```

**预期结果**:
- 吞吐量: > 1000 TPS
- 平均响应时间: < 50ms
- 错误率: < 0.1%

---

### 6.4 开发环境配置建议

#### 开发环境使用内存存储

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev

imaping:
  token:
    registry:
      redis:
        enabled: false  # 开发环境使用内存存储
      inMemory:
        cache: true
    accessToken:
      timeToKillInSeconds: 3600  # 1 小时(开发环境可短一些)
    scheduling:
      enabled: true
      repeatInterval: 60000  # 1 分钟清理一次

logging:
  level:
    com.imaping.token: DEBUG  # 开发环境启用详细日志
```

---

### 6.5 测试环境配置建议

#### 测试环境使用 Redis(模拟生产环境)

```yaml
# application-test.yml
spring:
  profiles:
    active: test
  data:
    redis:
      host: redis-test.example.com
      port: 6379
      password: ${REDIS_PASSWORD}

imaping:
  token:
    registry:
      redis:
        enabled: true
      core:
        enable-locking: true  # 测试分布式锁
    accessToken:
      timeToKillInSeconds: 7200
    scheduling:
      enabled: false

logging:
  level:
    com.imaping.token: INFO
```

---

## 附录

### A. 快速检查清单

#### 生产环境部署前检查

- [ ] Token 长度 ≥ 32 字符(256 位熵)
- [ ] 强制 HTTPS 传输
- [ ] Cookie 设置 `HttpOnly=true` 和 `Secure=true`
- [ ] 启用 Redis 存储(`imaping.token.registry.redis.enabled=true`)
- [ ] 多实例部署启用分布式锁(`enable-locking=true`)
- [ ] Token 过期时间合理(推荐 7200 秒)
- [ ] 日志不记录完整 Token
- [ ] 配置健康检查端点(`/actuator/health`)
- [ ] 配置监控和告警
- [ ] 进行压力测试验证性能

---

### B. 性能参考值

| 指标 | 内存存储 | Redis 单机 | Redis 集群 |
|------|---------|-----------|-----------|
| **Token 创建** | < 1 ms | 2-5 ms | 3-8 ms |
| **Token 查询** | < 0.1 ms | 1-3 ms | 2-5 ms |
| **Token 删除** | < 0.1 ms | 1-3 ms | 2-5 ms |
| **吞吐量(QPS)** | 50000+ | 10000+ | 20000+ |

*说明: 实际性能受服务器配置、网络延迟等因素影响*

---

### C. 相关文档链接

- [集成指南](integration.md) - Spring Security 集成和部署配置
- [架构文档](architecture.md) - 系统设计和组件详解
- [配置参考](configuration.md) - 完整配置项列表
- [API 使用指南](api-guide.md) - API 使用示例

---

**维护责任**: 架构团队
**最后更新**: 2025-10-12
**文档版本**: v1.0
**反馈渠道**: docs@example.com
