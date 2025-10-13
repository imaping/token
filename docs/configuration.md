# 配置参考

> **快速参考**: imaping-token 完整配置项列表和使用指南
> **最后更新**: 2025-10-12
> **版本**: 0.0.3-SNAPSHOT

---

## 目录

- [1. 配置概览](#1-配置概览)
- [2. Token 基础配置](#2-token-基础配置)
- [3. Token 注册表配置](#3-token-注册表配置)
  - [3.1 注册表核心配置](#31-注册表核心配置)
  - [3.2 内存注册表配置](#32-内存注册表配置)
  - [3.3 Redis 注册表配置](#33-redis-注册表配置)
  - [3.4 注册表清理器配置](#34-注册表清理器配置)
- [4. 访问令牌配置](#4-访问令牌配置)
- [5. 调度配置](#5-调度配置)
- [6. 配置场景示例](#6-配置场景示例)
  - [6.1 开发环境配置](#61-开发环境配置)
  - [6.2 生产环境配置](#62-生产环境配置)
  - [6.3 集群部署配置](#63-集群部署配置)
  - [6.4 性能优化配置](#64-性能优化配置)

---

## 1. 配置概览

imaping-token 使用 Spring Boot 的 `@ConfigurationProperties` 机制,所有配置项都以 `imaping.token.*` 为前缀。

### 配置方式

支持三种配置方式:

**方式 1: application.yml (推荐)**
```yaml
imaping:
  token:
    accessTokenName: access_token
    accessToken:
      timeToKillInSeconds: PT2H
```

**方式 2: application.properties**
```properties
imaping.token.accessTokenName=access_token
imaping.token.accessToken.timeToKillInSeconds=PT2H
```

**方式 3: 环境变量**
```bash
export IMAPING_TOKEN_ACCESSTOKENNAME=access_token
export IMAPING_TOKEN_ACCESSTOKEN_TIMETOKILLINSECONDS=PT2H
```

### 配置优先级

按优先级从高到低:
1. 命令行参数 (`--imaping.token.accessTokenName=xxx`)
2. 环境变量 (`IMAPING_TOKEN_ACCESSTOKENNAME`)
3. `application.yml` / `application.properties`
4. 代码中的默认值

---

## 2. Token 基础配置

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.accessTokenName` | `String` | `"access_token"` | Token 名称,用于 Cookie 和 Header 的键名。防止部署在同一域名下的多个系统 Token 冲突 | `"my_app_token"` |

**配置说明:**

- **accessTokenName**: Token 的标识名称,影响以下场景:
  - Cookie 中的键名: `Set-Cookie: {accessTokenName}={tokenValue}`
  - HTTP Header 中的键名: `Authorization: Bearer {tokenValue}` 或 `{accessTokenName}: {tokenValue}`
  - URL 参数中的键名: `?{accessTokenName}={tokenValue}`

**使用场景:**

当多个应用部署在同一域名下时,为避免 Token 冲突,应为每个应用配置不同的 `accessTokenName`。

**示例:**
```yaml
imaping:
  token:
    accessTokenName: admin_token  # 管理后台使用 "admin_token"
```

---

## 3. Token 注册表配置

Token 注册表 (TokenRegistry) 负责存储和管理 Token,支持内存和 Redis 两种存储方式。

### 3.1 注册表核心配置

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.registry.core.enableLocking` | `boolean` | `true` | 是否启用分布式锁。在分布式环境中,防止并发操作导致的数据不一致 | `true` |

**配置说明:**

- **enableLocking**:
  - `true`: 启用分布式锁,Token 操作 (add/update/delete) 将被锁保护
  - `false`: 禁用锁,适用于单机部署或对性能要求极高的场景
  - Redis 注册表会使用 Redis 的分布式锁实现
  - 内存注册表会使用 JVM 本地锁

**使用场景:**

- **生产环境 (集群部署)**: 必须启用 (`true`)
- **开发环境 (单机)**: 可以禁用 (`false`) 以提升性能
- **高并发场景**: 启用锁可能影响性能,需根据实际情况权衡

**示例:**
```yaml
imaping:
  token:
    registry:
      core:
        enableLocking: true  # 生产环境启用分布式锁
```

### 3.2 内存注册表配置

内存注册表使用 `ConcurrentHashMap` 存储 Token,适用于单机应用。

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.registry.inMemory.cache` | `boolean` | `true` | 是否启用缓存自动过期清理。使用 Caffeine 缓存,Token 过期后自动移除 | `true` |
| `imaping.token.registry.inMemory.initialCapacity` | `int` | `1000` | 内存存储的初始容量。预估的 Token 数量,避免频繁扩容 | `5000` |
| `imaping.token.registry.inMemory.loadFactor` | `int` | `1` | 负载因子。控制何时触发扩容,值越小越早扩容 | `1` |
| `imaping.token.registry.inMemory.concurrency` | `int` | `20` | 并发级别。预估的并发更新线程数,影响内部分段锁数量 | `50` |

**配置说明:**

- **cache**:
  - `true`: 使用 `CachingTokenRegistry` (基于 Caffeine),Token 过期后自动清理,无需后台定时任务
  - `false`: 使用 `DefaultTokenRegistry` (基于 ConcurrentHashMap),需要配合定时清理器使用

- **initialCapacity**:
  - 设置为预估的峰值 Token 数量可以减少扩容开销
  - 过小会导致频繁扩容,影响性能
  - 过大会浪费内存

- **loadFactor**:
  - 默认值 `1` 表示当元素数量达到容量时才扩容
  - 调大可以节省内存,调小可以减少哈希冲突

- **concurrency**:
  - 设置为预估的并发更新线程数
  - 过小会导致锁竞争,影响并发性能
  - 过大会浪费内存 (每个段都有独立的锁)

**使用场景:**

- **单机应用**: 启用内存注册表,禁用 Redis
- **开发环境**: 使用内存注册表,快速启动无外部依赖
- **高并发单机应用**: 调整 `concurrency` 参数优化性能

**示例:**
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: false  # 禁用 Redis,使用内存存储
      inMemory:
        cache: true
        initialCapacity: 5000     # 预估 5000 个 Token
        loadFactor: 1
        concurrency: 50           # 50 个并发线程
```

### 3.3 Redis 注册表配置

Redis 注册表使用 Redis 存储 Token,支持持久化和集群共享。

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.registry.redis.enabled` | `boolean` | `true` | 是否启用 Redis 注册表。启用后自动注册 `RedisTokenRegistry` Bean | `true` |

**配置说明:**

- **enabled**:
  - `true`: 启用 Redis 注册表 (`RedisTokenRegistry`),Token 存储在 Redis 中
  - `false`: 禁用 Redis 注册表,使用内存注册表 (`DefaultTokenRegistry` 或 `CachingTokenRegistry`)
  - 启用 Redis 后,内存注册表的配置将被忽略

**Redis 连接配置:**

Redis 注册表依赖 Spring Data Redis,需要配置 Redis 连接信息:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 0
      lettuce:
        pool:
          max-active: 20     # 最大连接数
          max-idle: 10       # 最大空闲连接
          min-idle: 5        # 最小空闲连接
          max-wait: 2000ms   # 获取连接的最大等待时间
```

**Redis Key 格式:**

Token 在 Redis 中的 Key 格式为:
```
imaping.token:{tokenId}:{userId}
```

**TTL 自动过期:**

Redis 注册表会根据 Token 的过期策略自动设置 TTL,Token 过期后 Redis 会自动删除,无需定时清理器。

**使用场景:**

- **分布式应用**: 必须启用 Redis 注册表,实现多节点 Token 共享
- **生产环境**: 推荐使用 Redis,支持持久化和高可用
- **集群部署**: 使用 Redis Cluster 或 Redis Sentinel 实现高可用

**示例:**
```yaml
imaping:
  token:
    registry:
      redis:
        enabled: true  # 启用 Redis 注册表

spring:
  data:
    redis:
      host: redis-cluster.example.com
      port: 6379
      password: ${REDIS_PASSWORD}  # 从环境变量读取密码
```

### 3.4 注册表清理器配置

注册表清理器 (TokenRegistryCleaner) 负责定期清理过期的 Token。

**注意**:
- 使用 **Redis 注册表** 时,无需配置清理器 (Redis TTL 自动过期)
- 使用 **内存注册表且 `cache=true`** 时,无需配置清理器 (Caffeine 自动清理)
- 使用 **内存注册表且 `cache=false`** 时,需要配置清理器

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.registry.cleaner.schedule.enabled` | `boolean` | `true` | 是否启用清理器定时任务 | `true` |
| `imaping.token.registry.cleaner.schedule.startDelay` | `String` (Duration) | `"PT10S"` | 启动延迟时间 (ISO-8601 格式)。应用启动后延迟多久开始第一次清理 | `"PT30S"` |
| `imaping.token.registry.cleaner.schedule.repeatInterval` | `String` (Duration) | `"PT1M"` | 清理间隔时间 (ISO-8601 格式)。每隔多久执行一次清理 | `"PT5M"` |
| `imaping.token.registry.cleaner.schedule.enabledOnHost` | `String` (Regex) | `".*"` | 主机名匹配模式 (正则表达式)。只有匹配的主机才会执行清理,用于集群环境下指定清理节点 | `"node-1"` |

**Duration 格式说明 (ISO-8601):**

| 示例 | 含义 |
|------|------|
| `PT10S` | 10 秒 |
| `PT1M` | 1 分钟 |
| `PT5M` | 5 分钟 |
| `PT1H` | 1 小时 |
| `PT2H30M` | 2 小时 30 分钟 |

**配置说明:**

- **enabled**:
  - `true`: 启用清理器,定时清理过期 Token
  - `false`: 禁用清理器,适用于使用 Redis 或 Caffeine 缓存的场景

- **startDelay**:
  - 应用启动后延迟一段时间再开始清理,避免启动时的性能开销
  - 建议设置为 10-30 秒

- **repeatInterval**:
  - 清理的频率,根据 Token 的过期时间和业务量调整
  - 过于频繁会增加 CPU 开销,过于稀疏会导致内存占用增加
  - 建议设置为 1-5 分钟

- **enabledOnHost**:
  - 集群环境下,可以指定只有某个节点执行清理,避免重复清理
  - 使用正则表达式匹配主机名
  - 默认 `".*"` 表示所有节点都执行清理

**使用场景:**

- **内存注册表 (cache=false)**: 必须启用清理器
- **Redis 注册表**: 无需启用清理器 (禁用即可)
- **Caffeine 缓存 (cache=true)**: 无需启用清理器 (禁用即可)
- **集群环境**: 使用 `enabledOnHost` 指定清理节点

**示例:**
```yaml
# 场景 1: 内存注册表 + 定时清理
imaping:
  token:
    registry:
      redis:
        enabled: false
      inMemory:
        cache: false  # 禁用 Caffeine 缓存
      cleaner:
        schedule:
          enabled: true
          startDelay: PT30S
          repeatInterval: PT5M

# 场景 2: Redis 注册表 (无需清理器)
imaping:
  token:
    registry:
      redis:
        enabled: true
      cleaner:
        schedule:
          enabled: false  # 禁用清理器

# 场景 3: Caffeine 缓存 (无需清理器)
imaping:
  token:
    registry:
      redis:
        enabled: false
      inMemory:
        cache: true  # 启用 Caffeine 缓存
      cleaner:
        schedule:
          enabled: false  # 禁用清理器

# 场景 4: 集群环境指定清理节点
imaping:
  token:
    registry:
      redis:
        enabled: false
      inMemory:
        cache: false
      cleaner:
        schedule:
          enabled: true
          enabledOnHost: "app-node-1"  # 只有 app-node-1 执行清理
```

---

## 4. 访问令牌配置

访问令牌 (AccessToken) 相关配置。

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.accessToken.timeToKillInSeconds` | `String` (Duration) | `"PT2H"` | Token 过期时间 (ISO-8601 格式)。Token 创建后多久过期 | `"PT4H"` |
| `imaping.token.accessToken.createAsJwt` | `boolean` | `false` | 是否创建为 JWT 格式的 Token。当前版本尚未实现,保留配置项 | `false` |

**配置说明:**

- **timeToKillInSeconds**:
  - Token 的生存时间 (TTL)
  - 对于 `TimeoutAccessToken` (自动续期 Token),每次使用都会刷新过期时间
  - 对于 `HardTimeoutToken` (固定时间 Token),从创建时间开始计算,不会刷新
  - 使用 ISO-8601 Duration 格式 (如 `PT2H` 表示 2 小时)

- **createAsJwt**:
  - 当前版本 (0.0.1) 尚未实现 JWT 支持
  - 保留配置项,计划在未来版本中实现
  - 目前请保持默认值 `false`

**使用场景:**

- **短期 Token (1-4 小时)**: 适用于 Web 应用,平衡安全性和用户体验
- **长期 Token (24 小时+)**: 适用于移动应用或桌面应用
- **极短期 Token (15-30 分钟)**: 适用于高安全性要求的场景

**示例:**
```yaml
# 场景 1: Web 应用 (2 小时)
imaping:
  token:
    accessToken:
      timeToKillInSeconds: PT2H

# 场景 2: 移动应用 (7 天)
imaping:
  token:
    accessToken:
      timeToKillInSeconds: PT168H  # 7 * 24 = 168 小时

# 场景 3: 高安全性场景 (30 分钟)
imaping:
  token:
    accessToken:
      timeToKillInSeconds: PT30M
```

---

## 5. 调度配置

调度配置 (SchedulingProperties) 用于控制定时任务的执行,目前主要用于清理器。

**注意**: 此配置继承自清理器配置,通常通过 `imaping.token.registry.cleaner.schedule.*` 配置。

| 配置项 | 类型 | 默认值 | 说明 | 示例 |
|--------|------|--------|------|------|
| `imaping.token.registry.cleaner.schedule.enabled` | `boolean` | `true` | 是否启用调度 | `true` |
| `imaping.token.registry.cleaner.schedule.startDelay` | `String` (Duration) | `"PT15S"` | 启动延迟 | `"PT30S"` |
| `imaping.token.registry.cleaner.schedule.repeatInterval` | `String` (Duration) | `"PT2M"` | 重复间隔 | `"PT5M"` |
| `imaping.token.registry.cleaner.schedule.enabledOnHost` | `String` (Regex) | `".*"` | 主机名匹配模式 | `"node-.*"` |

详细说明请参见 [3.4 注册表清理器配置](#34-注册表清理器配置)。

---

## 6. 配置场景示例

### 6.1 开发环境配置

**特点**: 单机部署、快速启动、无外部依赖、自动清理

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev

imaping:
  token:
    # Token 基础配置
    accessTokenName: dev_access_token

    # 访问令牌配置
    accessToken:
      timeToKillInSeconds: PT4H  # 开发环境 Token 有效期 4 小时
      createAsJwt: false

    # 注册表配置
    registry:
      # 禁用 Redis,使用内存存储
      redis:
        enabled: false

      # 内存注册表配置
      inMemory:
        cache: true              # 启用 Caffeine 缓存,自动清理
        initialCapacity: 1000
        loadFactor: 1
        concurrency: 20

      # 核心配置
      core:
        enableLocking: false     # 单机环境无需分布式锁

      # 禁用清理器 (Caffeine 自动清理)
      cleaner:
        schedule:
          enabled: false

# 日志配置 (可选)
logging:
  level:
    com.imaping.token: DEBUG
```

### 6.2 生产环境配置

**特点**: Redis 存储、分布式锁、持久化、高可用

```yaml
# application-prod.yml
spring:
  profiles:
    active: prod

  # Redis 配置
  data:
    redis:
      host: redis-cluster.example.com
      port: 6379
      password: ${REDIS_PASSWORD}     # 从环境变量读取
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20              # 最大连接数
          max-idle: 10                # 最大空闲连接
          min-idle: 5                 # 最小空闲连接
          max-wait: 2000ms            # 获取连接的最大等待时间
        shutdown-timeout: 100ms

imaping:
  token:
    # Token 基础配置
    accessTokenName: access_token

    # 访问令牌配置
    accessToken:
      timeToKillInSeconds: PT2H       # 生产环境 Token 有效期 2 小时
      createAsJwt: false

    # 注册表配置
    registry:
      # 启用 Redis 注册表
      redis:
        enabled: true

      # 核心配置
      core:
        enableLocking: true           # 启用分布式锁

      # 禁用清理器 (Redis TTL 自动过期)
      cleaner:
        schedule:
          enabled: false

# 日志配置
logging:
  level:
    com.imaping.token: INFO
```

### 6.3 集群部署配置

**特点**: Redis Cluster、多节点、指定清理节点

```yaml
# application-cluster.yml
spring:
  profiles:
    active: cluster

  # Redis Cluster 配置
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1.example.com:6379
          - redis-node-2.example.com:6379
          - redis-node-3.example.com:6379
          - redis-node-4.example.com:6379
          - redis-node-5.example.com:6379
          - redis-node-6.example.com:6379
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 50              # 集群环境增加连接数
          max-idle: 20
          min-idle: 10
          max-wait: 3000ms

imaping:
  token:
    # Token 基础配置
    accessTokenName: access_token

    # 访问令牌配置
    accessToken:
      timeToKillInSeconds: PT2H
      createAsJwt: false

    # 注册表配置
    registry:
      # 启用 Redis 注册表
      redis:
        enabled: true

      # 核心配置
      core:
        enableLocking: true           # 启用分布式锁

      # 禁用清理器 (Redis TTL 自动过期)
      cleaner:
        schedule:
          enabled: false

# 日志配置
logging:
  level:
    com.imaping.token: INFO
    com.imaping.token.api.registry: DEBUG  # 注册表操作日志
```

### 6.4 性能优化配置

**特点**: 调优 Redis 连接池、调整并发参数、优化过期时间

```yaml
# application-performance.yml
spring:
  profiles:
    active: performance

  # Redis 性能优化配置
  data:
    redis:
      host: redis.example.com
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 2000ms               # 缩短超时时间
      lettuce:
        pool:
          max-active: 100           # 高并发场景增加最大连接数
          max-idle: 50              # 增加空闲连接数,减少创建连接开销
          min-idle: 20              # 增加最小空闲连接,保持连接池活跃
          max-wait: 1000ms          # 缩短等待时间,快速失败
        shutdown-timeout: 100ms

imaping:
  token:
    # Token 基础配置
    accessTokenName: access_token

    # 访问令牌配置
    accessToken:
      timeToKillInSeconds: PT1H     # 缩短 Token 有效期,减少 Redis 存储压力
      createAsJwt: false

    # 注册表配置
    registry:
      # 启用 Redis 注册表
      redis:
        enabled: true

      # 核心配置
      core:
        enableLocking: true

      # 禁用清理器
      cleaner:
        schedule:
          enabled: false

# JVM 性能优化 (可选,启动参数配置)
# -Xmx2g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 日志配置 (减少日志输出)
logging:
  level:
    com.imaping.token: WARN
```

---

## 附录: 完整配置项列表

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `imaping.token.accessTokenName` | `String` | `"access_token"` | Token 名称 |
| `imaping.token.accessToken.timeToKillInSeconds` | `String` (Duration) | `"PT2H"` | Token 过期时间 |
| `imaping.token.accessToken.createAsJwt` | `boolean` | `false` | 是否创建为 JWT (未实现) |
| `imaping.token.registry.core.enableLocking` | `boolean` | `true` | 是否启用分布式锁 |
| `imaping.token.registry.inMemory.cache` | `boolean` | `true` | 是否启用 Caffeine 缓存 |
| `imaping.token.registry.inMemory.initialCapacity` | `int` | `1000` | 内存存储初始容量 |
| `imaping.token.registry.inMemory.loadFactor` | `int` | `1` | 负载因子 |
| `imaping.token.registry.inMemory.concurrency` | `int` | `20` | 并发级别 |
| `imaping.token.registry.redis.enabled` | `boolean` | `true` | 是否启用 Redis 注册表 |
| `imaping.token.registry.cleaner.schedule.enabled` | `boolean` | `true` | 是否启用清理器 |
| `imaping.token.registry.cleaner.schedule.startDelay` | `String` (Duration) | `"PT10S"` | 启动延迟 |
| `imaping.token.registry.cleaner.schedule.repeatInterval` | `String` (Duration) | `"PT1M"` | 清理间隔 |
| `imaping.token.registry.cleaner.schedule.enabledOnHost` | `String` (Regex) | `".*"` | 主机名匹配模式 |

---

**维护责任**: 架构团队
**更新频率**: 配置项变更时更新
**审核流程**: 技术委员会批准

**相关文档**:
- [架构文档](architecture.md)
- [API 使用指南](api-guide.md)
- [快速开始](quick-start.md)
