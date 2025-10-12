# imaping-token 架构图表集

> **文档版本**: v1.0
> **最后更新**: 2025-10-11
> **说明**: 本文档包含 imaping-token 系统的各种架构图表,使用 Mermaid 图表语言

---

## 目录

- [1. 模块依赖图](#1-模块依赖图)
- [2. Token 类型层次结构](#2-token-类型层次结构)
- [3. TokenRegistry 实现层次](#3-tokenregistry-实现层次)
- [4. Token 创建流程](#4-token-创建流程)
- [5. Token 验证流程](#5-token-验证流程)
- [6. 自动配置加载顺序](#6-自动配置加载顺序)
- [7. 部署架构](#7-部署架构)

---

## 1. 模块依赖图

```mermaid
graph TB
    parent[imaping-token-parent]
    deps[imaping-token-dependencies]
    config[imaping-configuration-model]
    core[imaping-token-core]
    api[imaping-token-api]
    redis[imaping-token-redis-registry]
    client[imaping-token-resource-client]

    parent --> deps
    parent --> config
    parent --> core
    parent --> api
    parent --> redis
    parent --> client

    deps --> spring[spring-boot-dependencies:3.5.6]

    config --> springboot[Spring Boot]
    config --> jackson[Jackson]

    core --> springboot
    core --> security[Spring Security Web]

    api --> core
    api --> config
    api --> caffeine[Caffeine]
    api --> commons[Commons Lang3/IO/Codec]

    redis --> api
    redis --> redislib[Spring Data Redis]

    client --> api
    client --> securitylib[Spring Security Config]

    style parent fill:#e1f5ff
    style deps fill:#fff3e0
    style config fill:#f3e5f5
    style core fill:#e8f5e9
    style api fill:#fff9c4
    style redis fill:#fce4ec
    style client fill:#e0f2f1
```

**说明**:
- **蓝色**: 父 POM
- **橙色**: 依赖管理模块
- **紫色**: 配置模型
- **绿色**: 核心模块
- **黄色**: API 模块
- **粉色**: Redis 实现
- **青色**: 资源客户端

---

## 2. Token 类型层次结构

```mermaid
classDiagram
    class Token {
        <<interface>>
        +getId() String
        +getCreationTime() ZonedDateTime
        +getCountOfUses() int
        +isExpired() Boolean
        +getExpirationPolicy() ExpirationPolicy
        +markTokenExpired() void
        +update() void
    }

    class AbstractToken {
        <<abstract>>
        -id String
        -expirationPolicy ExpirationPolicy
        -lastTimeUsed ZonedDateTime
        -previousTimeUsed ZonedDateTime
        -creationTime ZonedDateTime
        -countOfUses int
        -expired Boolean
        -authentication Authentication
        +update() void
        +isExpired() Boolean
    }

    class TimeoutAccessToken {
        <<interface>>
    }

    class DefaultTimeoutAccessToken {
        -PREFIX = "AT"
        +getId() String
    }

    class HardTimeoutToken {
        <<interface>>
        +getCode() String
        +getDescription() String
    }

    class DefaultHardTimeoutToken {
        -PREFIX = "ATT"
        -code String
        -description String
        +getId() String
    }

    Token <|.. AbstractToken
    AbstractToken <|-- TimeoutAccessToken
    AbstractToken <|-- HardTimeoutToken
    TimeoutAccessToken <|.. DefaultTimeoutAccessToken
    HardTimeoutToken <|.. DefaultHardTimeoutToken

    AbstractToken o-- ExpirationPolicy
    AbstractToken o-- Authentication

    class ExpirationPolicy {
        <<interface>>
        +isExpired(Token) Boolean
        +getTimeToLive() Long
        +getTimeToIdle() Long
    }

    class Authentication {
        -principal Principal
        -attributes Map
    }
```

**关键点**:
- `Token`: 顶层接口,定义基本契约
- `AbstractToken`: 抽象基类,封装通用逻辑
- `DefaultTimeoutAccessToken`: 自动续期 Token (PREFIX="AT")
- `DefaultHardTimeoutToken`: 固定时间 Token (PREFIX="ATT")

---

## 3. TokenRegistry 实现层次

```mermaid
classDiagram
    class TokenRegistry {
        <<interface>>
        +addToken(Token) Token
        +getToken(String) Token
        +getToken(String, Class) T
        +deleteToken(String) Token
        +updateToken(Token) void
        +getTokens() Collection
        +sessionCount() long
        +countSessionsFor(String) long
        +getSessionsFor(String) Collection
    }

    class AbstractTokenRegistry {
        <<abstract>>
        +addToken(Token) Token
        +getToken(String) Token
        +updateToken(Token) void
        #addTokenInternal(Token)* void
        #getToken(String, Predicate)* Token
        #deleteTokenInternal(String)* Token
    }

    class AbstractMapBasedTokenRegistry {
        <<abstract>>
        #tokenStore Map
        +getTokens() Collection
        +sessionCount() long
    }

    class DefaultTokenRegistry {
        -tokenStore ConcurrentHashMap
        +场景: 单机应用
        +特性: 快速、无持久化
    }

    class CachingTokenRegistry {
        -cache Caffeine
        +场景: 单机应用
        +特性: 带缓存优化
    }

    class RedisTokenRegistry {
        -redisTemplate TokenRedisTemplate
        +场景: 分布式应用
        +特性: 持久化、集群共享
    }

    TokenRegistry <|.. AbstractTokenRegistry
    AbstractTokenRegistry <|-- AbstractMapBasedTokenRegistry
    AbstractTokenRegistry <|-- RedisTokenRegistry
    AbstractMapBasedTokenRegistry <|-- DefaultTokenRegistry
    AbstractMapBasedTokenRegistry <|-- CachingTokenRegistry
```

**存储对比**:

| 实现类 | 存储 | 场景 | 持久化 | 集群 |
|--------|------|------|--------|------|
| DefaultTokenRegistry | ConcurrentHashMap | 单机 | ❌ | ❌ |
| CachingTokenRegistry | Caffeine | 单机 | ❌ | ❌ |
| RedisTokenRegistry | Redis | 分布式 | ✅ | ✅ |

---

## 4. Token 创建流程

```mermaid
sequenceDiagram
    participant Client as 业务代码
    participant Factory as TokenFactory
    participant Generator as UniqueTokenIdGenerator
    participant Policy as ExpirationPolicy
    participant Registry as TokenRegistry
    participant Storage as 存储 (Map/Redis)

    Client->>Factory: createToken(authentication)
    Factory->>Generator: getNewTokenId(length)
    Generator-->>Factory: tokenId

    Factory->>Policy: buildExpirationPolicy()
    Policy-->>Factory: expirationPolicy

    Factory->>Factory: 创建 Token 实例
    Note over Factory: - 设置 ID<br/>- 设置过期策略<br/>- 绑定认证信息

    Factory-->>Client: token

    Client->>Registry: addToken(token)
    Registry->>Storage: 持久化存储
    Storage-->>Registry: 成功
    Registry-->>Client: token
```

**关键步骤**:
1. 业务代码调用 `TokenFactory.createToken(authentication)`
2. 生成唯一 Token ID
3. 构建过期策略
4. 创建 Token 实例并设置属性
5. 添加到 `TokenRegistry`
6. 持久化到存储 (内存或 Redis)

---

## 5. Token 验证流程

```mermaid
sequenceDiagram
    participant Client as HTTP 请求
    participant Filter as TokenAuthenticationFilter
    participant Provider as TokenAuthenticationProvider
    participant Registry as TokenRegistry
    participant Token as Token
    participant Policy as ExpirationPolicy
    participant Context as SecurityContext

    Client->>Filter: HTTP Request (带 Token)

    Filter->>Filter: 提取 Token
    Note over Filter: 1. Header: Bearer <token><br/>2. Cookie: access_token<br/>3. Parameter: ?access_token=xxx

    Filter->>Provider: authenticate(tokenId)

    Provider->>Registry: getToken(tokenId)
    Registry-->>Provider: token

    alt Token 不存在
        Provider-->>Filter: null
        Filter-->>Client: 401 Unauthorized
    end

    Provider->>Token: isExpired()
    Token->>Policy: isExpired(token)
    Policy-->>Token: expired
    Token-->>Provider: expired

    alt Token 已过期
        Provider->>Registry: deleteToken(tokenId)
        Provider-->>Filter: null
        Filter->>Filter: 清除 Cookie (如果有)
        Filter-->>Client: 401 Unauthorized
    end

    Provider->>Token: update()
    Note over Token: 更新:<br/>- lastTimeUsed<br/>- countOfUses++

    Provider->>Registry: updateToken(token)

    Provider->>Provider: 构造 Authentication
    Provider-->>Filter: authentication

    Filter->>Context: setAuthentication(authentication)
    Filter-->>Client: 继续过滤器链
```

**验证流程**:
1. ✅ 提取 Token (Header > Cookie > Parameter)
2. ✅ 从 TokenRegistry 获取 Token
3. ✅ 验证过期状态
4. ✅ 更新使用统计
5. ✅ 构造 Authentication
6. ✅ 设置到 SecurityContext

**失败处理**:
- ❌ Token 不存在 → 401
- ❌ Token 已过期 → 删除 Token + 清除 Cookie → 401

---

## 6. 自动配置加载顺序

```mermaid
graph TD
    A[Spring Boot 启动] --> B[IMapingPropertiesConfiguration]
    B --> C[加载配置属性]
    C --> D{Redis 启用?}

    D -->|是| E[TokenConfig]
    D -->|否| F[TokenApiConfig]

    E --> E1[创建 RedisTokenRegistry]
    E1 --> F

    F --> F1[创建核心 Bean]
    F1 --> F2[tokenIdGenerator]
    F2 --> F3[lockRepository]
    F3 --> F4[expirationPolicy]
    F4 --> F5[tokenFactory]
    F5 --> F6{TokenRegistry 存在?}

    F6 -->|否| F7[创建 DefaultTokenRegistry]
    F6 -->|是| G
    F7 --> G

    G[TokenCoreAutoConfig] --> G1[userInfoContext]
    G1 --> G2[securityUserInfoContext]
    G2 --> G3[securityContextUtil]

    G3 --> H{调度启用?}
    H -->|是| I[TokenSchedulingConfiguration]
    H -->|否| J
    I --> I1[创建定时清理任务]
    I1 --> J

    J[ResourceClientConfig] --> J1[扫描 @Aware 组件]
    J1 --> K[TokenSecurityConfig]
    K --> K1[tokenAuthenticationProvider]
    K1 --> K2[tokenAuthenticationEntryPoint]
    K2 --> K3[apiFilterChain]
    K3 --> L[应用启动完成]

    style B fill:#e1f5ff
    style E fill:#fce4ec
    style F fill:#fff9c4
    style G fill:#e8f5e9
    style I fill:#fff3e0
    style K fill:#e0f2f1
```

**加载顺序说明**:

1. **IMapingPropertiesConfiguration**
   - 加载 `@ConfigurationProperties("imaping")`

2. **TokenConfig** (@AutoConfigureBefore TokenApiConfig)
   - 条件创建 `RedisTokenRegistry` (如果 `imaping.token.registry.redis.enabled=true`)

3. **TokenApiConfig** (@AutoConfigureBefore TokenCoreAutoConfig)
   - 创建核心 Bean: `tokenIdGenerator`, `lockRepository`, `expirationPolicy`, `tokenFactory`
   - 如果 `TokenRegistry` 不存在,创建 `DefaultTokenRegistry`

4. **TokenCoreAutoConfig**
   - 创建用户上下文相关 Bean

5. **TokenSchedulingConfiguration**
   - 创建定时清理任务 (如果启用)

6. **ResourceClientConfig**
   - 扫描 @Aware 组件

7. **TokenSecurityConfig**
   - 创建 Spring Security 配置

---

## 7. 部署架构

### 7.1 单机部署

```mermaid
graph TB
    subgraph "Spring Boot 应用"
        Filter[TokenAuthenticationFilter]
        Provider[TokenAuthenticationProvider]
        Registry[DefaultTokenRegistry]
        Storage[(ConcurrentHashMap)]
        Scheduler[Token 清理调度]

        Filter --> Provider
        Provider --> Registry
        Registry --> Storage
        Scheduler -.定时清理.-> Registry
    end

    Client[用户请求] -->|HTTP| Filter
    Filter -->|验证成功| Business[业务处理器]

    style Filter fill:#e0f2f1
    style Registry fill:#fff9c4
    style Storage fill:#e8f5e9
```

**特点**:
- ✅ 简单部署
- ✅ 低延迟
- ❌ 无法集群共享
- ❌ 重启丢失数据

### 7.2 分布式部署

```mermaid
graph TB
    LB[负载均衡器<br/>Nginx/ALB]

    subgraph "应用实例 1"
        Filter1[TokenAuthenticationFilter]
        Provider1[TokenAuthenticationProvider]
        Registry1[RedisTokenRegistry]
    end

    subgraph "应用实例 2"
        Filter2[TokenAuthenticationFilter]
        Provider2[TokenAuthenticationProvider]
        Registry2[RedisTokenRegistry]
    end

    subgraph "应用实例 N"
        Filter3[TokenAuthenticationFilter]
        Provider3[TokenAuthenticationProvider]
        Registry3[RedisTokenRegistry]
    end

    subgraph "Redis 集群"
        Master[(Redis Master)]
        Slave1[(Redis Slave 1)]
        Slave2[(Redis Slave 2)]

        Master -.复制.-> Slave1
        Master -.复制.-> Slave2
    end

    Client[用户请求] -->|HTTP| LB
    LB --> Filter1
    LB --> Filter2
    LB --> Filter3

    Registry1 --> Master
    Registry2 --> Master
    Registry3 --> Master

    style LB fill:#e1f5ff
    style Master fill:#fce4ec
    style Slave1 fill:#fff3e0
    style Slave2 fill:#fff3e0
```

**特点**:
- ✅ 集群共享 Token
- ✅ 高可用
- ✅ Redis TTL 自动过期
- ✅ 持久化
- ❌ Redis 依赖
- ❌ 网络延迟

### 7.3 网络拓扑

```mermaid
graph LR
    Internet[Internet] --> CDN[CDN<br/>静态资源]
    Internet --> LB[负载均衡器<br/>Layer 7]

    LB --> App1[App 实例 1<br/>10.0.1.10]
    LB --> App2[App 实例 2<br/>10.0.1.11]
    LB --> AppN[App 实例 N<br/>10.0.1.1X]

    App1 --> Redis[Redis 集群<br/>10.0.2.0/24]
    App2 --> Redis
    AppN --> Redis

    App1 --> DB[(应用数据库<br/>10.0.3.0/24)]
    App2 --> DB
    AppN --> DB

    style Internet fill:#e1f5ff
    style LB fill:#fff3e0
    style Redis fill:#fce4ec
    style DB fill:#e8f5e9
```

**网络分区**:
- **公网**: CDN + 负载均衡器
- **应用层**: `10.0.1.0/24`
- **缓存层**: `10.0.2.0/24` (Redis)
- **数据层**: `10.0.3.0/24` (数据库)

---

## 8. Token 过期策略对比

```mermaid
graph LR
    subgraph "TimeoutExpirationPolicy (自动续期)"
        T1[创建时间] --> T2[首次使用]
        T2 --> T3[第2次使用]
        T3 --> T4[第N次使用]
        T4 --> T5{超过空闲时间?}
        T5 -->|是| T6[过期]
        T5 -->|否| T7[续期]
        T7 --> T4
    end

    subgraph "HardTimeoutExpirationPolicy (固定时间)"
        H1[创建时间] --> H2[使用N次]
        H2 --> H3{超过存活时间?}
        H3 -->|是| H4[过期]
        H3 -->|否| H2
    end

    style T6 fill:#ffcdd2
    style T7 fill:#c8e6c9
    style H4 fill:#ffcdd2
```

**对比表**:

| 策略 | 判断依据 | 续期 | 适用场景 |
|------|----------|------|----------|
| **TimeoutExpirationPolicy** | `lastTimeUsed + timeToIdle < now` | ✅ 每次使用自动续期 | 用户会话 |
| **HardTimeoutExpirationPolicy** | `creationTime + timeToLive < now` | ❌ 固定时间失效 | 验证码、临时链接 |

---

## 9. 组件交互图

```mermaid
graph TB
    subgraph "业务层"
        Business[业务代码]
    end

    subgraph "API 层"
        Factory[TokenFactory]
        Registry[TokenRegistry]
        Cleaner[TokenRegistryCleaner]
    end

    subgraph "模型层"
        Token[Token]
        Policy[ExpirationPolicy]
        Auth[Authentication]
    end

    subgraph "存储层"
        Memory[(内存<br/>ConcurrentHashMap)]
        Redis[(Redis)]
    end

    subgraph "Security 层"
        Filter[TokenAuthenticationFilter]
        Provider[TokenAuthenticationProvider]
    end

    Business -->|创建 Token| Factory
    Business -->|查询 Token| Registry

    Factory -->|创建| Token
    Token -->|包含| Policy
    Token -->|包含| Auth

    Registry -->|存储| Memory
    Registry -->|存储| Redis

    Filter -->|提取 Token| Provider
    Provider -->|验证| Registry

    Cleaner -.定时清理.-> Registry

    style Business fill:#e1f5ff
    style Factory fill:#fff9c4
    style Registry fill:#fff9c4
    style Token fill:#f3e5f5
    style Filter fill:#e0f2f1
    style Memory fill:#e8f5e9
    style Redis fill:#fce4ec
```

---

## 10. 扩展点架构

```mermaid
graph TB
    subgraph "核心接口 (扩展点)"
        IToken[Token]
        IRegistry[TokenRegistry]
        IFactory[TokenFactory]
        IPolicy[ExpirationPolicy]
    end

    subgraph "默认实现"
        DToken[DefaultTimeoutAccessToken<br/>DefaultHardTimeoutToken]
        DRegistry[DefaultTokenRegistry<br/>RedisTokenRegistry]
        DFactory[TimeoutTokenDefaultFactory<br/>HardTimeoutTokenDefaultFactory]
        DPolicy[TimeoutExpirationPolicy<br/>HardTimeoutExpirationPolicy]
    end

    subgraph "自定义实现 (用户扩展)"
        CToken[CustomToken]
        CRegistry[DatabaseTokenRegistry]
        CFactory[CustomTokenFactory]
        CPolicy[SlidingWindowExpirationPolicy]
    end

    IToken -.默认实现.-> DToken
    IRegistry -.默认实现.-> DRegistry
    IFactory -.默认实现.-> DFactory
    IPolicy -.默认实现.-> DPolicy

    IToken -.用户扩展.-> CToken
    IRegistry -.用户扩展.-> CRegistry
    IFactory -.用户扩展.-> CFactory
    IPolicy -.用户扩展.-> CPolicy

    style IToken fill:#e1f5ff
    style IRegistry fill:#e1f5ff
    style IFactory fill:#e1f5ff
    style IPolicy fill:#e1f5ff

    style DToken fill:#c8e6c9
    style DRegistry fill:#c8e6c9
    style DFactory fill:#c8e6c9
    style DPolicy fill:#c8e6c9

    style CToken fill:#fff9c4
    style CRegistry fill:#fff9c4
    style CFactory fill:#fff9c4
    style CPolicy fill:#fff9c4
```

**扩展点**:
1. **Token**: 自定义 Token 类型 (如 RefreshToken)
2. **TokenRegistry**: 自定义存储后端 (如数据库)
3. **TokenFactory**: 自定义 Token 创建逻辑
4. **ExpirationPolicy**: 自定义过期策略 (如滑动窗口)

---

## 变更记录

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2025-10-11 | Architecture Team | 初始版本 - 架构图表集 |

---

**文档位置**: [`docs/architecture-diagrams.md`](architecture-diagrams.md)
