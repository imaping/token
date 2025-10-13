# imaping-token 架构文档

> **文档版本**: v4
> **最后更新**: 2025-10-12
> **适用版本**: 0.0.2-SNAPSHOT

---

## 目录

1. [模块架构](#1-模块架构)
2. [核心组件](#2-核心组件)
3. [Token 生命周期](#3-token-生命周期)
4. [扩展点](#4-扩展点)
5. [参考资料](#5-参考资料)

---

## 1. 模块架构

### 1.1 模块概述

imaping-token 采用多模块 Maven 项目结构,共包含 6 个核心模块:

| 模块 | 职责 | 发布 | 关键类 |
|------|------|------|-------|
| **imaping-token-dependencies** | 依赖版本统一管理 | ✅ | - |
| **imaping-configuration-model** | 配置属性模型定义 | ✅ | `IMapingConfigurationProperties` |
| **imaping-token-core** | 用户信息和安全上下文管理 | ✅ | `BaseUserInfo`, `SecurityContextUtil` |
| **imaping-token-api** | Token 管理核心 API 和实现 | ✅ | `Token`, `TokenRegistry`, `TokenFactory` |
| **imaping-token-redis-registry** | 基于 Redis 的 Token 存储实现 | ✅ | `RedisTokenRegistry` |
| **imaping-token-resource-client** | Spring Security 集成 | ✅ | `TokenAuthenticationFilter` |

### 1.2 模块依赖关系

```
imaping-token-parent
│
├── imaping-token-dependencies
│   └── [管理所有模块版本]
│
├── imaping-configuration-model
│   └── [定义配置属性]
│
├── imaping-token-core
│   └── [用户上下文和安全上下文]
│
├── imaping-token-api
│   ├── ← imaping-token-core
│   ├── ← imaping-configuration-model
│   └── [Token 管理核心逻辑]
│
├── imaping-token-redis-registry
│   ├── ← imaping-token-api
│   └── [Redis 存储实现]
│
└── imaping-token-resource-client
    ├── ← imaping-token-api
    └── [Spring Security 集成]
```

**编译顺序:**
1. `imaping-token-dependencies`
2. `imaping-configuration-model`
3. `imaping-token-core`
4. `imaping-token-api`
5. `imaping-token-redis-registry`
6. `imaping-token-resource-client`

### 1.3 模块详细说明

#### 1.3.1 imaping-token-dependencies

**职责**: 依赖版本统一管理 (BOM - Bill of Materials)

**核心功能**:
- 继承 `spring-boot-dependencies:3.5.6`
- 定义所有子模块版本 (通过 `${revision}` 变量)
- 统一管理第三方库版本 (Caffeine, Commons-Lang3 等)

#### 1.3.2 imaping-configuration-model

**职责**: 配置属性模型定义

**核心类**:
- `IMapingConfigurationProperties` - 主配置类 (`@ConfigurationProperties("imaping")`)
- `TokenConfigurationProperties` - Token 配置
- `TokenRegistryProperties` - 注册表配置
- `AccessTokenProperties` - 访问令牌配置

#### 1.3.3 imaping-token-core

**职责**: 用户信息和安全上下文管理

**核心类**:
- `UserInfo` (接口) - 用户信息基础接口
- `BaseUserInfo` - 基础用户信息实体
- `SecurityContextUtil` - 安全上下文工具

#### 1.3.4 imaping-token-api

**职责**: Token 管理核心 API 和实现 (最重要的模块)

**核心包结构**:
```
com.imaping.token.api
├── model/             # Token 模型
├── registry/          # Token 注册表
├── factory/           # Token 工厂
├── expiration/        # 过期策略
├── authentication/    # 认证组件
├── generator/         # ID 生成器
├── exception/         # 异常处理
├── config/            # 自动配置
└── common/            # 工具类
```

#### 1.3.5 imaping-token-redis-registry

**职责**: 基于 Redis 的 Token 存储实现

**核心类**:
- `RedisTokenRegistry` - Redis 注册表实现
- `TokenRedisTemplate` - Redis 模板接口

**条件激活**:
```yaml
imaping.token.registry.redis.enabled: true
```

**Redis Key 格式**:
```
imaping.token:{tokenId}:{userId}
```

#### 1.3.6 imaping-token-resource-client

**职责**: Spring Security 集成和 Token 认证

**核心类**:
- `TokenAuthenticationFilter` - Token 认证过滤器
- `TokenAuthenticationProvider` - 认证提供者
- `TokenAuthenticationEntryPoint` - 认证入口点 (401 处理)
- `TokenSecurityConfig` - Security 配置

---

## 2. 核心组件

### 2.1 Token 类型层次结构

```
Token (接口)
├── getId(): String                    # Token ID
├── getCreationTime(): ZonedDateTime  # 创建时间
├── getCountOfUses(): int             # 使用次数
├── isExpired(): Boolean              # 是否过期
├── getExpirationPolicy()             # 过期策略
├── markTokenExpired(): void          # 标记过期
└── update(): void                    # 更新使用状态
     │
     └─── AbstractToken (抽象类)
          ├── id: String
          ├── expirationPolicy: ExpirationPolicy
          ├── lastTimeUsed: ZonedDateTime
          ├── previousTimeUsed: ZonedDateTime
          ├── creationTime: ZonedDateTime
          ├── countOfUses: int
          ├── expired: Boolean
          └── authentication: Authentication
               │
               ├─── TimeoutAccessToken (接口)
               │    └── DefaultTimeoutAccessToken
               │         ├── PREFIX = "AT"
               │         ├── 过期策略: TimeoutExpirationPolicy
               │         └── 用途: 自动续期的访问令牌
               │
               └─── HardTimeoutToken (接口)
                    └── DefaultHardTimeoutToken
                         ├── PREFIX = "ATT"
                         ├── code: String (业务编码)
                         ├── description: String (描述)
                         ├── 过期策略: HardTimeoutExpirationPolicy
                         └── 用途: 固定时间令牌(验证码、临时链接)
```

**关键源文件**:
- [`Token`](imaping-token-api/src/main/java/com/imaping/token/api/model/Token.java:1)
- [`AbstractToken`](imaping-token-api/src/main/java/com/imaping/token/api/model/AbstractToken.java:1)
- [`DefaultTimeoutAccessToken`](imaping-token-api/src/main/java/com/imaping/token/api/model/DefaultTimeoutAccessToken.java:1)
- [`DefaultHardTimeoutToken`](imaping-token-api/src/main/java/com/imaping/token/api/model/DefaultHardTimeoutToken.java:1)

### 2.2 ExpirationPolicy 过期策略

```
ExpirationPolicy (接口)
├── isExpired(Token): Boolean     # 判断是否过期
├── getTimeToLive(): Long         # 获取存活时间 (TTL)
└── getTimeToIdle(): Long         # 获取空闲时间
     │
     └─── AbstractTokenExpirationPolicy (抽象类)
          │
          ├─── TimeoutExpirationPolicy
          │    ├── 判断逻辑: lastTimeUsed + timeToIdle < now
          │    ├── 特性: 每次使用自动续期
          │    └── 适用场景: 需要保持活跃的会话 Token
          │
          └─── HardTimeoutExpirationPolicy
               ├── 判断逻辑: creationTime + timeToLive < now
               ├── 特性: 固定时间后失效,不受使用影响
               └── 适用场景: 验证码、临时授权链接
```

**关键源文件**:
- [`ExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/ExpirationPolicy.java:1)
- [`TimeoutExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/TimeoutExpirationPolicy.java:1)
- [`HardTimeoutExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/HardTimeoutExpirationPolicy.java:1)

### 2.3 TokenRegistry 注册表

```
TokenRegistry (接口)
├── addToken(Token): Token                     # 添加 Token
├── getToken(String): Token                    # 获取 Token
├── getToken(String, Class<T>): T             # 按类型获取
├── deleteToken(String): Token                 # 删除 Token
├── updateToken(Token): void                   # 更新 Token
├── getTokens(): Collection<Token>             # 获取所有 Token
├── sessionCount(): long                       # 会话计数
├── countSessionsFor(String): long             # 统计用户会话数
└── getSessionsFor(String): Collection<Token>  # 获取用户会话
     │
     └─── AbstractTokenRegistry (抽象类)
          ├── 封装通用逻辑
          └── 定义抽象方法
               │
               ├─── AbstractMapBasedTokenRegistry
               │    ├── 基于 Map 的抽象实现
               │    └── DefaultTokenRegistry
               │         ├── 存储: ConcurrentHashMap
               │         ├── 场景: 单机应用
               │         └── 特性: 快速、无持久化
               │
               ├─── CachingTokenRegistry
               │    ├── 存储: Caffeine 缓存
               │    ├── 场景: 单机应用
               │    └── 特性: 带缓存优化
               │
               └─── RedisTokenRegistry
                    ├── 存储: Redis
                    ├── 场景: 分布式应用
                    └── 特性: 持久化、集群共享、TTL 自动过期
```

**对比表**:

| 实现类 | 存储方式 | 适用场景 | 持久化 | 集群支持 | TTL 自动过期 |
|--------|----------|----------|--------|----------|-------------|
| DefaultTokenRegistry | ConcurrentHashMap | 单机应用、开发环境 | ❌ | ❌ | ❌ |
| CachingTokenRegistry | Caffeine 缓存 | 单机应用 | ❌ | ❌ | ✅ |
| RedisTokenRegistry | Redis | 分布式应用、生产环境 | ✅ | ✅ | ✅ |

**关键源文件**:
- [`TokenRegistry`](imaping-token-api/src/main/java/com/imaping/token/api/registry/TokenRegistry.java:1)
- [`DefaultTokenRegistry`](imaping-token-api/src/main/java/com/imaping/token/api/registry/DefaultTokenRegistry.java:1)
- [`RedisTokenRegistry`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/RedisTokenRegistry.java:1)

### 2.4 TokenFactory 工厂

```
TokenFactory (接口)
├── getTokenType(): Class<? extends Token>           # 获取 Token 类型
├── createToken(Authentication): Token               # 创建 Token
└── createToken(String, Authentication): Token       # 创建指定 ID 的 Token
     │
     ├─── DefaultTokenFactory
     │    ├── 组合工厂
     │    ├── 支持多种 Token 类型
     │    └── 根据类型委托给具体工厂
     │
     ├─── TimeoutTokenFactory (接口)
     │    └── TimeoutTokenDefaultFactory
     │         └── 创建 DefaultTimeoutAccessToken
     │
     └─── HardTimeoutTokenFactory (接口)
          └── HardTimeoutTokenDefaultFactory
               └── 创建 DefaultHardTimeoutToken
```

**关键源文件**:
- [`TokenFactory`](imaping-token-api/src/main/java/com/imaping/token/api/factory/TokenFactory.java:1)
- [`DefaultTokenFactory`](imaping-token-api/src/main/java/com/imaping/token/api/factory/DefaultTokenFactory.java:1)
- [`TimeoutTokenDefaultFactory`](imaping-token-api/src/main/java/com/imaping/token/api/factory/TimeoutTokenDefaultFactory.java:1)
- [`HardTimeoutTokenDefaultFactory`](imaping-token-api/src/main/java/com/imaping/token/api/factory/HardTimeoutTokenDefaultFactory.java:1)

### 2.5 Authentication 认证机制

```
Authentication (类)
├── principal: Principal              # 主体
│   ├── id: String                   # 用户 ID
│   └── userInfo: BaseUserInfo       # 用户详细信息
│        ├── userId: Long
│        ├── username: String
│        ├── nickname: String
│        ├── email: String
│        ├── mobile: String
│        └── ...
└── attributes: Map<String, Object>   # 附加属性
```

**相关类**:
- [`Authentication`](imaping-token-api/src/main/java/com/imaping/token/api/authentication/Authentication.java:1)
- [`Principal`](imaping-token-api/src/main/java/com/imaping/token/api/authentication/principal/Principal.java:1)
- [`BaseUserInfo`](imaping-token-core/src/main/java/com/imaping/token/core/model/BaseUserInfo.java:1)
- [`DefaultTokenAuthentication`](imaping-token-api/src/main/java/com/imaping/token/api/authentication/DefaultTokenAuthentication.java:1)

---

## 3. Token 生命周期

### 3.1 Token 创建流程

```
┌─────────────┐
│   业务代码   │
└──────┬──────┘
       │ 1. 调用 TokenFactory.createToken(authentication)
       ▼
┌─────────────────────┐
│    TokenFactory     │
│ (工厂选择器)         │
└──────┬──────────────┘
       │ 2. 委托给具体工厂
       ▼
┌─────────────────────┐
│ TimeoutTokenFactory │
│ 或                   │
│ HardTimeoutToken    │
│ Factory             │
└──────┬──────────────┘
       │ 3. 创建 Token 实例
       │    - 生成唯一 ID (UniqueTokenIdGenerator)
       │    - 设置过期策略 (ExpirationPolicy)
       │    - 绑定认证信息 (Authentication)
       ▼
┌─────────────────────┐
│      Token          │
│ (DefaultTimeout     │
│  AccessToken 或      │
│  DefaultHardTimeout │
│  Token)             │
└──────┬──────────────┘
       │ 4. 添加到注册表
       ▼
┌─────────────────────┐
│   TokenRegistry     │
│ (DefaultToken       │
│  Registry 或         │
│  RedisToken         │
│  Registry)          │
└──────┬──────────────┘
       │ 5. 持久化存储
       ▼
┌─────────────────────┐
│  ConcurrentHashMap  │
│   或 Redis           │
└─────────────────────┘
```

**关键代码路径**:
1. 业务代码调用: `tokenFactory.createToken(authentication)`
2. 工厂选择: `DefaultTokenFactory.createToken()`
3. ID 生成: `DefaultUniqueTokenIdGenerator.getNewTokenId()`
4. 添加注册表: `tokenRegistry.addToken(token)`
5. 持久化: `DefaultTokenRegistry.addTokenInternal()` 或 `RedisTokenRegistry.addTokenInternal()`

### 3.2 Token 验证流程 (Spring Security 集成)

```
┌─────────────────────┐
│   HTTP 请求          │
│ (包含 Token)         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  TokenAuthenticationFilter          │ ← 过滤器链
│  extends OncePerRequestFilter       │
└──────┬──────────────────────────────┘
       │ 1. 提取 Token
       │    - 从 Header (Authorization: Bearer <token>)
       │    - 从 Cookie (access_token)
       │    - 从 RequestParameter (?access_token=xxx)
       ▼
┌─────────────────────┐
│ 提取到 Token ID      │
└──────┬──────────────┘
       │ 2. 构造认证请求
       ▼
┌─────────────────────────────────────┐
│  DefaultBearerTokenAuthenticationToken│
└──────┬──────────────────────────────┘
       │ 3. 调用 AuthenticationManager
       ▼
┌─────────────────────────────────────┐
│  TokenAuthenticationProvider        │
└──────┬──────────────────────────────┘
       │ 4. 从注册表获取 Token
       ▼
┌─────────────────────┐
│   TokenRegistry     │
│ .getToken(tokenId)  │
└──────┬──────────────┘
       │ 5. 获取 Token
       ▼
┌─────────────────────┐
│      Token          │
└──────┬──────────────┘
       │ 6. 验证过期状态
       ▼
┌─────────────────────┐
│ ExpirationPolicy    │
│ .isExpired(token)   │
└──────┬──────────────┘
       │ 7. 验证结果
       ▼
    ┌───┴───┐
    │       │
    ▼       ▼
 ✅ 通过   ❌ 失败
    │       │
    │       └──→ 返回 401 Unauthorized
    ▼
┌─────────────────────┐
│ 更新 Token 使用状态  │
│ token.update()      │
└──────┬──────────────┘
       │ 8. 构造 Authentication
       ▼
┌─────────────────────────────────────┐
│  DefaultTokenAuthentication         │
│  - principal: Principal             │
│  - token: Token                     │
└──────┬──────────────────────────────┘
       │ 9. 设置到 SecurityContext
       ▼
┌─────────────────────┐
│  SecurityContext    │
│  .setAuthentication │
│   (authentication)  │
└──────┬──────────────┘
       │ 10. 继续过滤器链
       ▼
┌─────────────────────┐
│   业务处理器         │
└─────────────────────┘
```

**关键代码路径**:
1. 过滤器: [`TokenAuthenticationFilter.doFilterInternal()`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/filter/TokenAuthenticationFilter.java:1)
2. 认证提供者: [`TokenAuthenticationProvider.authenticate()`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/authentication/TokenAuthenticationProvider.java:1)
3. 注册表查询: `tokenRegistry.getToken(tokenId)`
4. 过期验证: `token.isExpired()`
5. 更新使用状态: `token.update()` → 更新 `lastTimeUsed`, `countOfUses`

### 3.3 Token 清理流程

```
┌─────────────────────────────────────┐
│  TokenSchedulingConfiguration       │ ← 调度配置
│  @EnableScheduling                  │
└──────┬──────────────────────────────┘
       │ 1. 配置定时任务
       │    (基于 imaping.token.scheduling.*)
       ▼
┌─────────────────────┐
│   定时调度器         │
│ @Scheduled          │
└──────┬──────────────┘
       │ 2. 触发清理任务
       ▼
┌─────────────────────────────────────┐
│  TokenRegistryCleaner               │
└──────┬──────────────────────────────┘
       │ 3. 清理过期 Token
       ▼
┌─────────────────────┐
│   TokenRegistry     │
│ .getTokens()        │
└──────┬──────────────┘
       │ 4. 遍历所有 Token
       ▼
┌─────────────────────┐
│   检查每个 Token     │
└──────┬──────────────┘
       │ 5. 判断是否过期
       ▼
┌─────────────────────┐
│ ExpirationPolicy    │
│ .isExpired(token)   │
└──────┬──────────────┘
       │ 6. 删除过期 Token
       ▼
┌─────────────────────┐
│   TokenRegistry     │
│ .deleteToken(id)    │
└─────────────────────┘
```

**关键代码路径**:
1. 调度配置: [`TokenSchedulingConfiguration`](imaping-token-api/src/main/java/com/imaping/token/api/config/TokenSchedulingConfiguration.java:1)
2. 清理器: [`DefaultTokenRegistryCleaner.clean()`](imaping-token-api/src/main/java/com/imaping/token/api/registry/DefaultTokenRegistryCleaner.java:1)

**配置项**:
```yaml
imaping:
  token:
    scheduling:
      enabled: true                 # 启用调度
      pool-size: 5                  # 线程池大小
      repeatInterval: 120000        # 清理间隔 (毫秒)
      startDelay: 15000             # 启动延迟 (毫秒)
```

---

## 4. 扩展点

### 4.1 自定义 Token 类型

通过继承 `AbstractToken` 和实现 `Token` 接口,可以创建自定义的 Token 类型。

**步骤 1: 定义 Token 接口**
```java
public interface RefreshToken extends Token {
    String getRefreshTokenId();
    ZonedDateTime getRefreshExpirationTime();
}
```

**步骤 2: 实现 Token**
```java
public class DefaultRefreshToken extends AbstractToken implements RefreshToken {
    private static final String PREFIX = "RT";
    private String refreshTokenId;
    private ZonedDateTime refreshExpirationTime;

    @Override
    public String getId() {
        return PREFIX + super.getId();
    }

    // 实现其他方法...
}
```

**步骤 3: 实现 TokenFactory**
```java
@Component
public class RefreshTokenFactory implements TokenFactory {

    @Override
    public Class<? extends Token> getTokenType() {
        return RefreshToken.class;
    }

    @Override
    public Token createToken(Authentication authentication) {
        DefaultRefreshToken token = new DefaultRefreshToken();
        token.setAuthentication(authentication);
        token.setExpirationPolicy(buildExpirationPolicy());
        token.setRefreshTokenId(generateRefreshId());
        return token;
    }

    // 实现其他方法...
}
```

**步骤 4: 注册到 Spring**
```java
@Configuration
public class CustomTokenConfig {

    @Bean
    public TokenFactory refreshTokenFactory() {
        return new RefreshTokenFactory();
    }
}
```

### 4.2 自定义 ExpirationPolicy

通过继承 `AbstractTokenExpirationPolicy`,可以实现自定义的过期策略。

**示例: 滑动窗口过期策略**
```java
public class SlidingWindowExpirationPolicy extends AbstractTokenExpirationPolicy {

    private final Duration windowSize;
    private final int maxUses;

    @Override
    public boolean isExpired(Token token) {
        // 滑动窗口策略: 在时间窗口内使用次数不超过限制
        ZonedDateTime windowStart = ZonedDateTime.now().minus(windowSize);

        if (token.getLastTimeUsed().isBefore(windowStart)) {
            return true;  // 超出时间窗口
        }

        if (token.getCountOfUses() > maxUses) {
            return true;  // 超出使用次数
        }

        return false;
    }

    @Override
    public Long getTimeToLive() {
        return windowSize.toSeconds();
    }

    @Override
    public Long getTimeToIdle() {
        return windowSize.toSeconds();
    }
}
```

**使用示例**:
```java
@Bean
public ExpirationPolicyBuilder customExpirationPolicyBuilder() {
    return new ExpirationPolicyBuilder() {
        @Override
        public ExpirationPolicy buildTokenExpirationPolicy() {
            return new SlidingWindowExpirationPolicy(
                Duration.ofHours(1),  // 1 小时窗口
                100                    // 最多 100 次使用
            );
        }
    };
}
```

### 4.3 自定义 TokenRegistry

通过继承 `AbstractTokenRegistry`,可以实现基于数据库或其他存储后端的 Token 注册表。

**示例: 数据库存储实现**
```java
public class DatabaseTokenRegistry extends AbstractTokenRegistry {

    private final TokenRepository tokenRepository;  // JPA Repository

    @Override
    protected void addTokenInternal(Token token) throws Exception {
        TokenEntity entity = convertToEntity(token);
        tokenRepository.save(entity);
    }

    @Override
    public Token getToken(String tokenId, Predicate<Token> predicate) {
        TokenEntity entity = tokenRepository.findById(tokenId).orElse(null);
        if (entity == null) {
            return null;
        }

        Token token = convertToToken(entity);
        return predicate.test(token) ? token : null;
    }

    @Override
    public Token deleteTokenInternal(String tokenId) {
        Token token = getToken(tokenId);
        if (token != null) {
            tokenRepository.deleteById(tokenId);
        }
        return token;
    }

    // 实现其他方法...
}
```

**注册到 Spring**:
```java
@Configuration
public class CustomTokenRegistryConfig {

    @Bean
    @ConditionalOnProperty(name = "imaping.token.registry.database.enabled", havingValue = "true")
    public TokenRegistry databaseTokenRegistry(TokenRepository tokenRepository) {
        return new DatabaseTokenRegistry(tokenRepository);
    }
}
```

### 4.4 自定义 Security 配置

通过继承 `TokenSecurityConfig`,可以自定义 Spring Security 配置。

```java
@Configuration
@EnableWebSecurity
public class CustomSecurityConfig extends TokenSecurityConfig {

    @Override
    protected String[] getPermitAntMatchers() {
        return new String[]{
            "/public/**",
            "/login",
            "/register",
            "/health",
            "/actuator/health"
        };
    }

    @Override
    protected String[] getAuthenticatedAntMatchers() {
        return new String[]{
            "/api/**",
            "/admin/**",
            "/user/**"
        };
    }

    @Bean
    @Override
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        SecurityFilterChain chain = super.apiFilterChain(http);

        // 添加自定义配置
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://example.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 5. 参考资料

### 5.1 详细文档

- **[快速入门指南](quick-start.md)** - 30 分钟快速上手
- **[编码规范](architecture/coding-standards.md)** - 开发者必读
- **[技术栈](architecture/tech-stack.md)** - 技术选型和工具链
- **[源码结构](architecture/source-tree.md)** - 源码组织和包结构

### 5.2 架构文档碎片

详细的架构文档已分片存储在 `docs/architecture/` 目录:

- **[1-系统概述.md](architecture/1-系统概述.md)** - 系统简介和设计原则
- **[2-技术栈.md](architecture/2-技术栈.md)** - 详细技术栈说明
- **[3-模块架构.md](architecture/3-模块架构.md)** - 模块详细说明
- **[4-核心组件.md](architecture/4-核心组件.md)** - 核心组件详解
- **[5-数据流与交互.md](architecture/5-数据流与交互.md)** - 数据流和交互流程
- **[6-部署架构.md](architecture/6-部署架构.md)** - 部署架构和配置
- **[7-安全架构.md](architecture/7-安全架构.md)** - 安全设计和最佳实践
- **[8-扩展点.md](architecture/8-扩展点.md)** - 扩展点和自定义指南
- **[9-技术决策记录.md](architecture/9-技术决策记录.md)** - 技术选型决策

### 5.3 产品需求文档

- **[PRD 主文档](prd.md)** - 产品需求文档
- **[PRD 碎片](prd/)** - 分片存储的详细 PRD

### 5.4 外部资源

- **Spring Boot 文档**: [https://docs.spring.io/spring-boot/docs/3.5.6/reference/](https://docs.spring.io/spring-boot/docs/3.5.6/reference/)
- **Spring Security 文档**: [https://docs.spring.io/spring-security/reference/](https://docs.spring.io/spring-security/reference/)
- **Spring Data Redis 文档**: [https://docs.spring.io/spring-data/redis/reference/](https://docs.spring.io/spring-data/redis/reference/)

---

**文档维护**: imaping-token 团队
**更新频率**: 架构变更时更新
**问题反馈**: 请通过项目 Issue 提交
**最后更新**: 2025-10-12
