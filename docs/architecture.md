# imaping-token 架构文档

> **文档版本**: v1.0
> **最后更新**: 2025-10-11
> **项目版本**: 0.0.1-SNAPSHOT
> **适用范围**: 棕地系统现代化

---

## 目录

- [1. 系统概述](#1-系统概述)
- [2. 技术栈](#2-技术栈)
- [3. 模块架构](#3-模块架构)
- [4. 核心组件](#4-核心组件)
- [5. 数据流与交互](#5-数据流与交互)
- [6. 部署架构](#6-部署架构)
- [7. 安全架构](#7-安全架构)
- [8. 扩展点](#8-扩展点)
- [9. 技术决策记录](#9-技术决策记录)

---

## 1. 系统概述

### 1.1 系统简介

**imaping-token** 是一个企业级的 Token 管理和认证系统,提供统一的 Token 生命周期管理、多种存储后端支持和完整的 Spring Security 集成。系统设计遵循 SOLID 原则,采用模块化架构,支持单机和分布式部署场景。

**核心能力:**
- Token 生命周期管理 (创建、存储、检索、更新、删除)
- 灵活的过期策略 (自动续期、固定时间)
- 多存储后端 (内存、Redis)
- Spring Security 深度集成
- 分布式会话管理

### 1.2 系统边界

```
┌─────────────────────────────────────────────────────────────┐
│                     imaping-token 系统                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  输入边界:                                                   │
│  ├─ HTTP 请求 (Header/Cookie/Param 中的 Token)              │
│  ├─ API 调用 (TokenRegistry 接口)                           │
│  └─ Spring Security 认证请求                                │
│                                                             │
│  输出边界:                                                   │
│  ├─ Token 验证结果                                          │
│  ├─ Authentication 对象                                     │
│  └─ SecurityContext 填充                                    │
│                                                             │
│  依赖边界:                                                   │
│  ├─ Spring Boot 3.5.6                                       │
│  ├─ Spring Security 6.x                                     │
│  ├─ Redis (可选)                                            │
│  └─ Servlet API 6.0                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 关键设计原则

**DRY (Don't Repeat Yourself):**
- 抽象基类封装通用逻辑 (AbstractToken, AbstractTokenRegistry)
- 配置统一管理 (IMapingConfigurationProperties)

**KISS (Keep It Simple):**
- 接口定义简洁明了 (Token, TokenRegistry)
- 默认实现开箱即用

**SOLID:**
- **单一职责**: 每个模块专注特定功能
- **开闭原则**: 通过接口和抽象类支持扩展
- **里氏替换**: Token 层次结构可替换
- **接口隔离**: 小而专一的接口设计
- **依赖倒置**: 依赖抽象而非具体实现

---

## 2. 技术栈

### 2.1 核心技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.5.6 | 应用框架 |
| Spring Security | 6.x | 安全框架 |
| Spring Data Redis | 3.x | Redis 集成 |
| Jackson | 2.x | JSON 序列化 |
| Maven | 3.x | 构建工具 |

### 2.2 第三方库

| 库 | 版本 | 用途 |
|-----|------|------|
| Caffeine | - | 内存缓存 |
| Lombok | - | 代码简化 |
| Commons Lang3 | - | 工具类 |
| Commons Codec | - | 编码工具 |
| Commons IO | 2.11.0 | IO 工具 |
| Jool | 0.9.14 | 函数式工具 |

### 2.3 存储后端

| 后端 | 场景 | 特性 |
|------|------|------|
| 内存 (ConcurrentHashMap) | 单机应用 | 快速、无持久化 |
| Redis | 分布式应用 | 持久化、集群共享、TTL 自动过期 |

---

## 3. 模块架构

### 3.1 模块依赖图

```
imaping-token-parent (父POM)
│
├── imaping-token-dependencies (依赖管理)
│   └── [管理所有模块版本]
│
├── imaping-configuration-model (配置模型)
│   └── [定义配置属性]
│
├── imaping-token-core (核心模型)
│   └── [用户上下文和安全上下文]
│
├── imaping-token-api (核心API)
│   ├── ← imaping-token-core
│   ├── ← imaping-configuration-model
│   └── [Token管理核心逻辑]
│
├── imaping-token-redis-registry (Redis实现)
│   ├── ← imaping-token-api
│   └── [Redis存储实现]
│
└── imaping-token-resource-client (资源客户端)
    ├── ← imaping-token-api
    └── [Spring Security集成]
```

### 3.2 模块职责

#### 3.2.1 imaping-token-dependencies

**职责**: 依赖版本统一管理

**关键文件**:
- [pom.xml](imaping-token-dependencies/pom.xml)

**提供内容**:
- 继承 `spring-boot-dependencies:3.5.6`
- 定义所有子模块版本 (通过 `${revision}` 变量)

**依赖关系**:
```
└── spring-boot-dependencies:3.5.6
```

#### 3.2.2 imaping-configuration-model

**职责**: 配置属性模型定义

**核心类**:
- [`IMapingConfigurationProperties`](imaping-configuration-model/src/main/java/com/imaping/token/configuration/IMapingConfigurationProperties.java) - 主配置类 (@ConfigurationProperties("imaping"))
- [`TokenConfigurationProperties`](imaping-configuration-model/src/main/java/com/imaping/token/configuration/model/token/TokenConfigurationProperties.java) - Token 配置
- [`TokenRegistryProperties`](imaping-configuration-model/src/main/java/com/imaping/token/configuration/model/token/TokenRegistryProperties.java) - 注册表配置
- [`AccessTokenProperties`](imaping-configuration-model/src/main/java/com/imaping/token/configuration/model/token/AccessTokenProperties.java) - 访问令牌配置

**自动配置**:
- `IMapingPropertiesConfiguration`

**依赖关系**:
```
├── spring-boot
├── spring-boot-configuration-processor
└── jackson-annotations
```

#### 3.2.3 imaping-token-core

**职责**: 用户信息和安全上下文管理

**核心类**:
```
com.imaping.token.core.model
├── UserInfo (接口) - 用户信息基础接口
├── BaseUserInfo - 基础用户信息实体
├── SecurityUserInfo - 安全用户信息实体
├── UserInfoContext (接口) - 用户信息上下文
├── DefaultUserInfoContext - 默认实现
├── SecurityUserInfoContext (接口) - 安全用户信息上下文
└── DefaultSecurityUserInfoContext - 默认实现
```

**工具类**:
- [`SecurityContextUtil`](imaping-token-core/src/main/java/com/imaping/token/core/util/SecurityContextUtil.java:1) - 安全上下文工具

**自动配置**:
- [`TokenCoreAutoConfig`](imaping-token-core/src/main/java/com/imaping/token/core/TokenCoreAutoConfig.java:1)

**依赖关系**:
```
├── spring-boot
├── spring-security-web
└── jackson-annotations
```

#### 3.2.4 imaping-token-api

**职责**: Token 管理核心 API 和实现

**核心包结构**:
```
com.imaping.token.api
├── model/             # Token 模型
├── registry/          # Token 注册表
├── factory/           # Token 工厂
├── expiration/        # 过期策略
├── authentication/    # 认证组件
├── generator/         # ID 生成器
├── lock/              # 锁管理
├── exception/         # 异常处理
├── config/            # 自动配置
└── common/            # 工具类
```

**自动配置**:
- [`TokenApiConfig`](imaping-token-api/src/main/java/com/imaping/token/api/config/TokenApiConfig.java:1) - 核心配置类
- [`TokenSchedulingConfiguration`](imaping-token-api/src/main/java/com/imaping/token/api/config/TokenSchedulingConfiguration.java:1) - 调度配置

**依赖关系**:
```
├── imaping-token-core
├── imaping-configuration-model
├── jackson (datatype-jsr310, core, annotations)
├── caffeine
├── commons-io:2.11.0
├── commons-lang3
├── commons-codec
├── spring-tx
└── spring-integration-core
```

#### 3.2.5 imaping-token-redis-registry

**职责**: 基于 Redis 的 Token 存储实现

**核心类**:
- [`RedisTokenRegistry`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/RedisTokenRegistry.java:1) - Redis 注册表实现
- [`TokenRedisTemplate`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/TokenRedisTemplate.java:1) - Redis 模板接口
- [`DefaultTokenRedisTemplate`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/DefaultTokenRedisTemplate.java:1) - 默认实现

**自动配置**:
- [`TokenConfig`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/config/TokenConfig.java:1)

**条件激活**:
```yaml
imaping.token.registry.redis.enabled: true
```

**Redis Key 格式**:
```
imaping.token:{tokenId}:{userId}
```

**依赖关系**:
```
├── imaping-token-api
└── spring-boot-starter-data-redis
```

#### 3.2.6 imaping-token-resource-client

**职责**: Spring Security 集成和 Token 认证

**核心类**:
```
com.imaping.token.resource.client
├── authentication/
│   ├── TokenAuthenticationProvider - 认证提供者
│   └── TokenAuthenticationEntryPoint - 认证入口点
├── filter/
│   └── TokenAuthenticationFilter - Token 认证过滤器
├── aware/
│   └── CurrentUserAutoAware - 当前用户自动注入
└── config/
    ├── ResourceClientConfig - 资源客户端配置
    └── TokenSecurityConfig - Security 配置
```

**自动配置**:
- [`ResourceClientConfig`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/config/ResourceClientConfig.java:1)
- [`TokenSecurityConfig`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/config/TokenSecurityConfig.java:1)

**依赖关系**:
```
├── imaping-token-api
├── spring-security-web
├── spring-security-config
└── jakarta.servlet-api:6.0.0 (provided)
```

### 3.3 编译顺序

Maven Reactor 构建顺序:
```
1. imaping-token-dependencies
2. imaping-configuration-model
3. imaping-token-core
4. imaping-token-api
5. imaping-token-redis-registry
6. imaping-token-resource-client
```

---

## 4. 核心组件

### 4.1 Token 类型层次结构

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

### 4.2 ExpirationPolicy 过期策略

```
ExpirationPolicy (接口)
├── isExpired(Token): Boolean     # 判断是否过期
├── getTimeToLive(): Long         # 获取存活时间(TTL)
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

**构建器模式**:
```
ExpirationPolicyBuilder (接口)
├── TimeoutExpirationPolicyBuilder
│   └── 构建 TimeoutExpirationPolicy
│
├── HardTimeoutExpirationPolicyBuilder
│   └── 构建 HardTimeoutExpirationPolicy
│
└── HardTimeoutExpirationPolicyDefaultBuilder
    └── 构建默认 HardTimeoutExpirationPolicy
```

**关键源文件**:
- [`ExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/ExpirationPolicy.java:1)
- [`TimeoutExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/TimeoutExpirationPolicy.java:1)
- [`HardTimeoutExpirationPolicy`](imaping-token-api/src/main/java/com/imaping/token/api/expiration/HardTimeoutExpirationPolicy.java:1)

### 4.3 TokenRegistry 注册表

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
| DefaultTokenRegistry | ConcurrentHashMap | 单机应用 | ❌ | ❌ | ❌ |
| CachingTokenRegistry | Caffeine 缓存 | 单机应用 | ❌ | ❌ | ✅ |
| RedisTokenRegistry | Redis | 分布式应用 | ✅ | ✅ | ✅ |

**关键源文件**:
- [`TokenRegistry`](imaping-token-api/src/main/java/com/imaping/token/api/registry/TokenRegistry.java:1)
- [`DefaultTokenRegistry`](imaping-token-api/src/main/java/com/imaping/token/api/registry/DefaultTokenRegistry.java:1)
- [`RedisTokenRegistry`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/RedisTokenRegistry.java:1)

### 4.4 TokenFactory 工厂

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

### 4.5 Authentication 认证机制

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
- [`DefaultBearerTokenAuthenticationToken`](imaping-token-api/src/main/java/com/imaping/token/api/authentication/DefaultBearerTokenAuthenticationToken.java:1)

### 4.6 ID 生成器

```
UniqueTokenIdGenerator (接口)
└── getNewTokenId(int length): String         # 生成唯一 ID
     │
     └─── DefaultUniqueTokenIdGenerator
          ├── 组合生成器
          └── 使用 RandomStringGenerator

RandomStringGenerator (接口)
└── Base64RandomStringGenerator
     └── 生成 Base64 编码的随机字符串

NumericGenerator (接口)
└── LongNumericGenerator (接口)
     └── DefaultLongNumericGenerator
          └── 生成长整型数字
```

**关键源文件**:
- [`UniqueTokenIdGenerator`](imaping-token-api/src/main/java/com/imaping/token/api/generator/UniqueTokenIdGenerator.java:1)
- [`DefaultUniqueTokenIdGenerator`](imaping-token-api/src/main/java/com/imaping/token/api/generator/DefaultUniqueTokenIdGenerator.java:1)
- [`Base64RandomStringGenerator`](imaping-token-api/src/main/java/com/imaping/token/api/generator/Base64RandomStringGenerator.java:1)

---

## 5. 数据流与交互

### 5.1 Token 创建流程

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
2. 工厂选择: [`DefaultTokenFactory.createToken()`](imaping-token-api/src/main/java/com/imaping/token/api/factory/DefaultTokenFactory.java:1)
3. ID 生成: [`DefaultUniqueTokenIdGenerator.getNewTokenId()`](imaping-token-api/src/main/java/com/imaping/token/api/generator/DefaultUniqueTokenIdGenerator.java:1)
4. 添加注册表: `tokenRegistry.addToken(token)`
5. 持久化: [`DefaultTokenRegistry.addTokenInternal()`](imaping-token-api/src/main/java/com/imaping/token/api/registry/DefaultTokenRegistry.java:1) 或 [`RedisTokenRegistry.addTokenInternal()`](imaping-token-redis-registry/src/main/java/com/imaping/token/redis/registry/RedisTokenRegistry.java:1)

### 5.2 Token 验证流程 (Spring Security 集成)

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

### 5.3 Token 清理流程

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
      repeatInterval: 120000        # 清理间隔(毫秒)
      startDelay: 15000             # 启动延迟(毫秒)
```

### 5.4 自动配置加载顺序

```
1. IMapingPropertiesConfiguration
   └── 加载 @ConfigurationProperties("imaping")
        │
        ├─ IMapingConfigurationProperties
        ├─ TokenConfigurationProperties
        ├─ TokenRegistryProperties
        └─ AccessTokenProperties

2. TokenConfig (@AutoConfigureBefore TokenApiConfig)
   └── 条件创建 RedisTokenRegistry
       (条件: imaping.token.registry.redis.enabled=true)

3. TokenApiConfig (@AutoConfigureBefore TokenCoreAutoConfig)
   ├── tokenIdGenerator (UniqueTokenIdGenerator)
   ├── tokenRegistryLockRepository (LockRepository)
   ├── accessTokenExpirationPolicy (ExpirationPolicyBuilder)
   ├── hardTimeoutExpirationPolicy (HardTimeoutExpirationPolicyBuilder)
   ├── timeoutTokenFactory (TimeoutTokenFactory)
   ├── hardTimeoutTokenFactory (HardTimeoutTokenFactory)
   ├── defaultTokenFactory (TokenFactory)
   ├── tokenRegistry (TokenRegistry) - 默认内存实现
   └── tokenUserInfoContext (UserInfoContext)

4. TokenCoreAutoConfig
   ├── userInfoContext (UserInfoContext) - 如果未定义
   ├── securityUserInfoContext (SecurityUserInfoContext)
   └── securityContextUtil (SecurityContextUtil)

5. TokenSchedulingConfiguration
   └── Token 清理调度任务

6. ResourceClientConfig
   └── 扫描 @Aware 组件

7. TokenSecurityConfig
   ├── tokenAuthenticationProvider (TokenAuthenticationProvider)
   ├── tokenAuthenticationEntryPoint (TokenAuthenticationEntryPoint)
   └── apiFilterChain (SecurityFilterChain)
```

**自动配置文件位置**:
- `META-INF/spring.factories` (Spring Boot 2.7 之前)
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 2.7+)

---

## 6. 部署架构

### 6.1 单机部署

```
┌─────────────────────────────────────────────────────┐
│                   Spring Boot 应用                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │       TokenRegistry (内存)                  │   │
│  │  - DefaultTokenRegistry                     │   │
│  │  - 存储: ConcurrentHashMap                  │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │       TokenAuthenticationFilter             │   │
│  │  - Token 提取和验证                          │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │       TokenSchedulingConfiguration          │   │
│  │  - 定时清理过期 Token                        │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
                         │
                         │ HTTP Request
                         ▼
                    ┌─────────┐
                    │ 用户请求 │
                    └─────────┘
```

**配置示例**:
```yaml
imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: false            # 使用内存存储
      inMemory:
        cache: true               # 启用 Caffeine 缓存
        initialCapacity: 1000
        loadFactor: 1
        concurrency: 20
    accessToken:
      timeToKillInSeconds: 7200   # 2小时
    scheduling:
      enabled: true
      repeatInterval: 120000      # 2分钟清理一次
```

**适用场景**:
- 低并发应用
- 开发/测试环境
- 不需要跨实例共享会话

### 6.2 分布式部署 (Redis)

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Spring Boot 实例1│     │  Spring Boot 实例2│     │  Spring Boot 实例N│
├──────────────────┤     ├──────────────────┤     ├──────────────────┤
│ RedisTokenRegistry│     │ RedisTokenRegistry│     │ RedisTokenRegistry│
└────────┬─────────┘     └────────┬─────────┘     └────────┬─────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                                  ▼
                      ┌───────────────────────┐
                      │      Redis 集群        │
                      ├───────────────────────┤
                      │  Key 格式:             │
                      │  imaping.token:        │
                      │    {tokenId}:{userId}  │
                      │                       │
                      │  TTL 自动过期          │
                      └───────────────────────┘
                                  │
                                  │
                      ┌───────────▼───────────┐
                      │    负载均衡器 (Nginx)   │
                      └───────────┬───────────┘
                                  │ HTTP Request
                                  ▼
                              ┌─────────┐
                              │ 用户请求 │
                              └─────────┘
```

**配置示例**:
```yaml
imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: true             # 使用 Redis 存储
      core:
        enable-locking: true      # 启用分布式锁
    accessToken:
      timeToKillInSeconds: 7200   # 2小时
    scheduling:
      enabled: false              # Redis 自动过期,无需定时清理

spring:
  data:
    redis:
      host: redis-cluster.example.com
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

**适用场景**:
- 高并发应用
- 多实例集群部署
- 需要跨实例共享会话
- 需要持久化 Token

**优势**:
- ✅ 集群共享: 所有实例共享同一份 Token 数据
- ✅ 持久化: Redis 持久化机制保证数据不丢失
- ✅ 自动过期: Redis TTL 自动清理过期 Token
- ✅ 高可用: Redis 集群支持主从复制和哨兵

### 6.3 网络拓扑

```
                          Internet
                             │
                             ▼
                    ┌────────────────┐
                    │   负载均衡器    │
                    │   (Nginx/ALB)  │
                    └────────┬───────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
       ┌──────────┐   ┌──────────┐   ┌──────────┐
       │ App 实例1 │   │ App 实例2 │   │ App 实例N │
       └─────┬────┘   └─────┬────┘   └─────┬────┘
             │              │              │
             └──────────────┼──────────────┘
                            │
                   ┌────────▼────────┐
                   │   Redis 集群     │
                   │  (主从/哨兵)     │
                   └─────────────────┘
```

---

## 7. 安全架构

### 7.1 安全组件

```
┌─────────────────────────────────────────────────────┐
│               Spring Security 集成                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  TokenSecurityConfig                                │
│  ├── SecurityFilterChain                            │
│  │   ├── permitAll: /public/**, /health            │
│  │   ├── authenticated: /api/**, /admin/**         │
│  │   └── csrf: disabled (可配置)                   │
│  │                                                  │
│  ├── TokenAuthenticationFilter                      │
│  │   ├── 提取 Token (Header/Cookie/Parameter)       │
│  │   ├── 验证 Token                                 │
│  │   └── 设置 SecurityContext                       │
│  │                                                  │
│  ├── TokenAuthenticationProvider                    │
│  │   ├── 从 TokenRegistry 获取 Token               │
│  │   ├── 验证过期状态                               │
│  │   └── 返回 Authentication                        │
│  │                                                  │
│  └── TokenAuthenticationEntryPoint                  │
│      └── 返回 401 Unauthorized                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 7.2 Token 提取策略

**优先级顺序**:
1. **HTTP Header** (最高优先级)
   ```
   Authorization: Bearer <token>
   ```

2. **Cookie**
   ```
   Cookie: access_token=<token>
   ```

3. **Request Parameter** (最低优先级)
   ```
   GET /api/users?access_token=<token>
   ```

**关键源文件**: [`TokenAuthenticationFilter`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/filter/TokenAuthenticationFilter.java:1)

### 7.3 Token 失效处理

```
Token 验证失败
     │
     ├─ Token 不存在
     │   └─ 返回 401 Unauthorized
     │
     ├─ Token 已过期
     │   ├─ 删除 Token (从 TokenRegistry)
     │   ├─ 清除 Cookie (如果是从 Cookie 提取)
     │   └─ 返回 401 Unauthorized
     │
     └─ Token 格式错误
         └─ 返回 401 Unauthorized
```

**关键代码路径**:
- 验证逻辑: [`TokenAuthenticationProvider.authenticate()`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/authentication/TokenAuthenticationProvider.java:1)
- 入口点: [`TokenAuthenticationEntryPoint.commence()`](imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/authentication/TokenAuthenticationEntryPoint.java:1)

### 7.4 安全最佳实践

#### 7.4.1 Token 长度和强度

```java
// 默认配置
UniqueTokenIdGenerator tokenIdGenerator = new DefaultUniqueTokenIdGenerator(
    new Base64RandomStringGenerator(),
    32  // 32字符长度 (256位熵)
);
```

**建议**:
- ✅ Token 长度 ≥ 32 字符
- ✅ 使用 Base64 编码随机字节
- ✅ 避免使用可预测的 ID 生成算法

#### 7.4.2 过期时间配置

```yaml
imaping:
  token:
    accessToken:
      timeToKillInSeconds: 7200  # 2小时 (生产环境建议值)
```

**建议**:
- ✅ 短期 Token: 1-4 小时
- ✅ 长期 Token: 使用 Refresh Token 机制 (需自行实现)
- ❌ 避免: 无限期 Token

#### 7.4.3 HTTPS 传输

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_PASSWORD}
    key-store-type: PKCS12
```

**建议**:
- ✅ 生产环境强制使用 HTTPS
- ✅ 设置 Cookie 的 `Secure` 属性
- ✅ 设置 Cookie 的 `HttpOnly` 属性

#### 7.4.4 CSRF 保护

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        );
    return http.build();
}
```

**建议**:
- ✅ 对于传统表单应用,启用 CSRF 保护
- ✅ 对于 RESTful API (Bearer Token),可禁用 CSRF
- ✅ 使用 `SameSite=Strict` Cookie 属性

---

## 8. 扩展点

### 8.1 自定义 Token 类型

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

### 8.2 自定义 ExpirationPolicy

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
                Duration.ofHours(1),  // 1小时窗口
                100                    // 最多100次使用
            );
        }
    };
}
```

### 8.3 自定义 TokenRegistry

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

    @Override
    public long deleteAll() {
        long count = tokenRepository.count();
        tokenRepository.deleteAll();
        return count;
    }

    @Override
    public boolean deleteToken(String tokenId, Predicate<Token> predicate) {
        Token token = getToken(tokenId);
        if (token != null && predicate.test(token)) {
            tokenRepository.deleteById(tokenId);
            return true;
        }
        return false;
    }

    @Override
    public Collection<Token> getTokens() {
        return tokenRepository.findAll().stream()
            .map(this::convertToToken)
            .collect(Collectors.toList());
    }

    @Override
    public Stream<String> getTokenIds() {
        return tokenRepository.findAllIds().stream();
    }

    @Override
    public long sessionCount() {
        return tokenRepository.count();
    }

    @Override
    public Stream<Token> stream() {
        return tokenRepository.findAll().stream()
            .map(this::convertToToken);
    }
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

### 8.4 自定义 Security 配置

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

### 8.5 自定义 Token 清理策略

```java
@Component
public class CustomTokenRegistryCleaner implements TokenRegistryCleaner {

    private final TokenRegistry tokenRegistry;
    private final int batchSize = 100;

    @Override
    @Scheduled(cron = "0 */10 * * * *")  // 每10分钟执行一次
    public void clean() {
        long totalCleaned = 0;

        try (Stream<Token> tokenStream = tokenRegistry.stream()) {
            List<Token> batch = new ArrayList<>();

            tokenStream
                .filter(Token::isExpired)
                .forEach(token -> {
                    batch.add(token);

                    if (batch.size() >= batchSize) {
                        totalCleaned += cleanBatch(batch);
                        batch.clear();
                    }
                });

            // 清理剩余的 Token
            if (!batch.isEmpty()) {
                totalCleaned += cleanBatch(batch);
            }
        }

        log.info("Token cleanup completed. Cleaned {} tokens.", totalCleaned);
    }

    private long cleanBatch(List<Token> tokens) {
        return tokens.stream()
            .map(Token::getId)
            .filter(id -> tokenRegistry.deleteToken(id) != null)
            .count();
    }
}
```

---

## 9. 技术决策记录

### 9.1 为什么使用 ConcurrentHashMap 而非普通 HashMap?

**决策**: 使用 `ConcurrentHashMap` 作为内存存储的默认实现

**原因**:
1. **线程安全**: 多线程并发访问 Token 注册表
2. **性能**: 分段锁机制,读写性能优于 `Hashtable`
3. **无锁读取**: 读操作完全无锁,适合读多写少的场景

**权衡**:
- ✅ 优点: 高并发性能、线程安全
- ❌ 缺点: 内存占用稍高于普通 HashMap

### 9.2 为什么支持多种 Token 提取方式?

**决策**: 支持从 Header、Cookie、RequestParameter 提取 Token

**原因**:
1. **兼容性**: 支持不同客户端类型 (浏览器、移动 App、API 客户端)
2. **灵活性**: 根据场景选择最合适的传输方式
3. **渐进迁移**: 从 Cookie 迁移到 Bearer Token 时保持兼容

**优先级**:
1. **Header** (推荐): RESTful API 标准方式
2. **Cookie**: 浏览器 Web 应用
3. **Parameter**: 特殊场景 (如 WebSocket 握手、第三方回调)

### 9.3 为什么设计两种 Token 类型?

**决策**: 提供 `TimeoutAccessToken` 和 `HardTimeoutToken` 两种类型

**原因**:
1. **TimeoutAccessToken (自动续期)**
   - 用途: 用户会话管理
   - 特性: 保持活跃自动续期
   - 场景: Web 应用登录态

2. **HardTimeoutToken (固定时间)**
   - 用途: 临时令牌 (验证码、一次性链接)
   - 特性: 固定时间后失效
   - 场景: 邮箱验证、密码重置、临时分享链接

**设计模式**: 策略模式 (ExpirationPolicy)

### 9.4 为什么使用 Redis 而非数据库?

**决策**: 优先推荐 Redis 作为分布式存储

**原因**:
1. **性能**: Redis 内存存储,访问速度远超数据库
2. **TTL 支持**: 原生支持自动过期,无需定时清理任务
3. **分布式**: 天然支持集群和主从复制
4. **简单**: 无需设计表结构和索引

**权衡**:
- ✅ 优点: 高性能、自动过期、分布式友好
- ❌ 缺点: 额外的 Redis 依赖、内存成本

**扩展**: 系统提供扩展点,支持自定义数据库存储 (参见 [8.3 自定义 TokenRegistry](#83-自定义-tokenregistry))

### 9.5 为什么禁用部分 Spring Security 默认行为?

**决策**: 默认禁用 CSRF、Session 管理

**原因**:
1. **CSRF**: RESTful API 使用 Bearer Token,不依赖 Cookie,无需 CSRF 保护
2. **Session**: 基于 Token 的无状态认证,不需要 Session

**可配置**: 用户可通过继承 `TokenSecurityConfig` 自定义行为

### 9.6 为什么使用 `@AutoConfigureBefore`?

**决策**: 使用 `@AutoConfigureBefore` 控制自动配置顺序

**原因**:
1. **依赖关系**: `TokenConfig` 必须在 `TokenApiConfig` 之前,确保 Redis 注册表优先创建
2. **条件装配**: `@ConditionalOnMissingBean` 依赖加载顺序
3. **可预测**: 明确的加载顺序,避免随机失败

**顺序**:
```
TokenConfig → TokenApiConfig → TokenCoreAutoConfig
```

---

## 附录

### A. 配置属性完整列表

参见 [`docs/configuration.md`](configuration.md) (待创建)

### B. API 使用示例

参见 [`docs/api-guide.md`](api-guide.md) (待创建)

### C. 快速入门指南

参见 [`docs/quick-start.md`](quick-start.md) (待创建)

### D. 最佳实践

参见 [`docs/best-practices.md`](best-practices.md) (待创建)

### E. 集成指南

参见 [`docs/integration.md`](integration.md) (待创建)

### F. 故障排查

参见 [`docs/troubleshooting.md`](troubleshooting.md) (待创建)

---

## 变更记录

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2025-10-11 | Architecture Team | 初始版本 - 棕地系统架构文档化 |

---

## 文档维护

**维护责任**: 架构团队
**更新频率**: 每次重大架构变更
**审核流程**: 架构评审委员会批准

**文档位置**: [`docs/architecture.md`](architecture.md)
