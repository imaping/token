# 源码结构

> **快速参考**: imaping-token 项目源码目录结构和包组织
> **最后更新**: 2025-10-12
> **项目版本**: 0.0.6-SNAPSHOT

---

## 1. 项目根目录结构

```
imaping-token/
├── imaping-token-dependencies/          # 依赖管理模块
├── imaping-configuration-model/         # 配置模型模块
├── imaping-token-core/                  # 核心模型模块
├── imaping-token-api/                   # 核心 API 模块
├── imaping-token-redis-registry/        # Redis 实现模块
├── imaping-token-resource-client/       # 资源客户端模块
├── imaping-token-test/                  # 测试应用模块
├── docs/                                # 项目文档
│   ├── architecture.md                  # 架构文档
│   ├── architecture/                    # 架构文档分片
│   │   ├── coding-standards.md         # 编码规范
│   │   ├── tech-stack.md               # 技术栈
│   │   └── source-tree.md              # 源码结构 (本文档)
│   ├── prd.md                          # 产品需求文档
│   ├── prd/                            # PRD 文档分片
│   └── stories/                        # 开发故事
├── pom.xml                              # 父 POM 文件
├── .gitignore                           # Git 忽略配置
├── CLAUDE.md                            # Claude AI 配置
└── README.md                            # 项目说明
```

---

## 2. 模块详细结构

### 2.1 imaping-token-dependencies

**职责**: 统一管理所有依赖版本

```
imaping-token-dependencies/
├── src/
│   └── (无源码,纯依赖管理)
└── pom.xml                              # BOM (Bill of Materials)
    ├── <parent> spring-boot-dependencies:3.5.6
    ├── <dependencyManagement>           # 管理所有子模块和第三方库版本
    └── <properties>                     # 版本号变量
```

**关键配置**:
- 继承 `spring-boot-dependencies:3.5.6`
- 定义 `${revision}` 变量统一子模块版本
- 管理第三方库版本 (Caffeine, Commons-Lang3, etc.)

---

### 2.2 imaping-configuration-model

**职责**: 配置属性模型定义

```
imaping-configuration-model/
├── src/main/java/com/imaping/token/configuration/
│   ├── IMapingConfigurationProperties.java        # @ConfigurationProperties("imaping")
│   ├── IMapingPropertiesConfiguration.java        # 自动配置类
│   └── model/
│       ├── token/                                 # Token 相关配置
│       │   ├── TokenConfigurationProperties.java  # Token 主配置
│       │   ├── TokenRegistryProperties.java       # 注册表配置
│       │   ├── TokenRegistryCoreProperties.java   # 注册表核心配置
│       │   ├── AccessTokenProperties.java         # 访问令牌配置
│       │   ├── InMemoryTokenRegistryProperties.java  # 内存注册表配置
│       │   ├── RedisTokenRegistryProperties.java     # Redis 注册表配置
│       │   ├── SchedulingProperties.java             # 调度配置
│       │   ├── ScheduledJobProperties.java           # 调度任务配置
│       │   ├── CaptchaProperties.java                # 验证码配置
│       │   └── CloudProperties.java                  # 云配置
│       ├── attachment/                            # 附件相关配置
│       │   ├── AttachmentProperties.java
│       │   ├── MinioProperties.java
│       │   └── SftpProperties.java
│       ├── cas/                                   # CAS 相关配置
│       │   └── CasProperties.java
│       ├── district/                              # 行政区划配置
│       │   └── DistrictProperties.java
│       └── elasticsearch/                         # Elasticsearch 配置
│           ├── ElasticSearchProperties.java
│           ├── MetricbeatProperties.java
│           └── SystemAccessProperties.java
├── src/main/resources/
│   └── META-INF/
│       └── spring-configuration-metadata.json     # 配置元数据 (IDE 提示)
└── pom.xml
```

**核心类**:
- `IMapingConfigurationProperties` - 主配置类,绑定 `imaping.*` 配置项
- `TokenConfigurationProperties` - Token 配置,包含注册表、访问令牌、调度等配置

---

### 2.3 imaping-token-core

**职责**: 用户信息和安全上下文管理

```
imaping-token-core/
├── src/main/java/com/imaping/token/core/
│   ├── TokenCoreAutoConfig.java                   # 自动配置类
│   ├── model/                                     # 数据模型
│   │   ├── UserInfo.java                          # 用户信息接口
│   │   ├── BaseUserInfo.java                      # 基础用户信息实体
│   │   ├── SecurityUserInfo.java                  # 安全用户信息实体
│   │   ├── UserInfoContext.java                   # 用户信息上下文接口
│   │   ├── DefaultUserInfoContext.java            # 默认实现
│   │   ├── SecurityUserInfoContext.java           # 安全用户信息上下文接口
│   │   └── DefaultSecurityUserInfoContext.java    # 默认实现
│   └── util/                                      # 工具类
│       └── SecurityContextUtil.java               # 安全上下文工具
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── pom.xml
```

**核心接口**:
- `UserInfo` - 用户信息基础接口
- `UserInfoContext` - 用户信息上下文管理

**关键工具**:
- `SecurityContextUtil` - 提供便捷的 SecurityContext 访问方法

---

### 2.4 imaping-token-api

**职责**: Token 管理核心 API 和实现 (最重要的模块)

```
imaping-token-api/
├── src/main/java/com/imaping/token/api/
│   ├── config/                                    # 配置类
│   │   ├── TokenApiConfig.java                    # 核心配置类
│   │   └── TokenSchedulingConfiguration.java      # 调度配置
│   │
│   ├── model/                                     # Token 模型
│   │   ├── Token.java                             # Token 接口
│   │   ├── AbstractToken.java                     # Token 抽象基类
│   │   ├── TimeoutAccessToken.java                # 自动续期 Token 接口
│   │   ├── DefaultTimeoutAccessToken.java         # 默认实现
│   │   ├── HardTimeoutToken.java                  # 固定时间 Token 接口
│   │   └── DefaultHardTimeoutToken.java           # 默认实现
│   │
│   ├── registry/                                  # Token 注册表
│   │   ├── TokenRegistry.java                     # 注册表接口
│   │   ├── AbstractTokenRegistry.java             # 抽象基类
│   │   ├── AbstractMapBasedTokenRegistry.java     # Map 基础抽象类
│   │   ├── DefaultTokenRegistry.java              # 内存实现 (ConcurrentHashMap)
│   │   ├── CachingTokenRegistry.java              # 缓存实现 (Caffeine)
│   │   ├── TokenRegistryCleaner.java              # 清理器接口
│   │   └── DefaultTokenRegistryCleaner.java       # 默认清理实现
│   │
│   ├── factory/                                   # Token 工厂
│   │   ├── TokenFactory.java                      # 工厂接口
│   │   ├── DefaultTokenFactory.java               # 默认组合工厂
│   │   ├── TimeoutTokenFactory.java               # 自动续期 Token 工厂接口
│   │   ├── TimeoutTokenDefaultFactory.java        # 默认实现
│   │   ├── HardTimeoutTokenFactory.java           # 固定时间 Token 工厂接口
│   │   └── HardTimeoutTokenDefaultFactory.java    # 默认实现
│   │
│   ├── expiration/                                # 过期策略
│   │   ├── ExpirationPolicy.java                  # 过期策略接口
│   │   ├── AbstractTokenExpirationPolicy.java     # 抽象基类
│   │   ├── TimeoutExpirationPolicy.java           # 自动续期策略
│   │   ├── HardTimeoutExpirationPolicy.java       # 固定时间策略
│   │   ├── ExpirationPolicyBuilder.java           # 构建器接口
│   │   ├── TimeoutExpirationPolicyBuilder.java    # 自动续期策略构建器
│   │   ├── HardTimeoutExpirationPolicyBuilder.java        # 固定时间策略构建器
│   │   └── HardTimeoutExpirationPolicyDefaultBuilder.java # 默认构建器
│   │
│   ├── authentication/                            # 认证组件
│   │   ├── Authentication.java                    # 认证信息类
│   │   ├── AuthenticationAwareToken.java          # 认证感知 Token 接口
│   │   ├── DefaultTokenAuthentication.java        # 默认认证实现
│   │   ├── DefaultBearerTokenAuthenticationToken.java  # Bearer Token 认证
│   │   ├── TokenUserInfoContext.java              # Token 用户信息上下文
│   │   └── principal/                             # 主体信息
│   │       └── Principal.java                     # 主体接口
│   │
│   ├── generator/                                 # ID 生成器
│   │   ├── UniqueTokenIdGenerator.java            # 唯一 Token ID 生成器接口
│   │   ├── DefaultUniqueTokenIdGenerator.java     # 默认实现
│   │   ├── RandomStringGenerator.java             # 随机字符串生成器接口
│   │   ├── Base64RandomStringGenerator.java       # Base64 随机字符串生成器
│   │   ├── NumericGenerator.java                  # 数字生成器接口
│   │   ├── LongNumericGenerator.java              # 长整型生成器接口
│   │   └── DefaultLongNumericGenerator.java       # 默认实现
│   │
│   ├── lock/                                      # 锁管理
│   │   ├── LockRepository.java                    # 锁仓库接口
│   │   └── ... (锁实现类)
│   │
│   ├── exception/                                 # 异常处理
│   │   ├── TokenException.java                    # Token 基础异常
│   │   ├── TokenNotFoundException.java            # Token 不存在异常
│   │   ├── TokenExpiredException.java             # Token 过期异常
│   │   └── ... (其他异常类)
│   │
│   ├── boot/                                      # Spring Boot 支持
│   │   ├── ConditionalOnMatchingHostname.java     # 主机名匹配条件
│   │   └── MatchingHostnameCondition.java         # 条件实现
│   │
│   └── common/                                    # 工具类
│       ├── BeanCondition.java                     # Bean 条件工具
│       ├── BeanSupplier.java                      # Bean 供应器
│       ├── Cleanable.java                         # 可清理接口
│       ├── FunctionUtils.java                     # 函数式工具
│       ├── RegexUtils.java                        # 正则工具
│       ├── ResourceUtils.java                     # 资源工具
│       └── SpringExpressionLanguageValueResolver.java  # SpEL 解析器
│
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── src/test/java/                                 # 单元测试
└── pom.xml
```

**核心设计模式**:
- **策略模式**: `ExpirationPolicy` 及其实现
- **工厂模式**: `TokenFactory` 及其实现
- **模板方法**: `AbstractToken`, `AbstractTokenRegistry`
- **构建器模式**: `ExpirationPolicyBuilder`

---

### 2.5 imaping-token-redis-registry

**职责**: 基于 Redis 的 Token 存储实现

```
imaping-token-redis-registry/
├── src/main/java/com/imaping/token/redis/registry/
│   ├── config/                                    # 配置类
│   │   └── TokenConfig.java                       # Redis Token 配置
│   │
│   ├── RedisTokenRegistry.java                    # Redis 注册表实现
│   ├── TokenRedisTemplate.java                    # Redis 模板接口
│   └── DefaultTokenRedisTemplate.java             # 默认实现
│
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── pom.xml
```

**核心类**:
- `RedisTokenRegistry` - Redis 存储实现,支持 TTL 自动过期
- `TokenRedisTemplate` - 封装 Redis 操作的模板类

**Redis Key 格式**:
```
imaping.token:{tokenId}:{userId}
```

**条件激活**:
```yaml
imaping.token.registry.redis.enabled: true
```

---

### 2.6 imaping-token-resource-client

**职责**: Spring Security 集成和 Token 认证

```
imaping-token-resource-client/
├── src/main/java/com/imaping/token/resource/client/
│   ├── config/                                    # 配置类
│   │   ├── ResourceClientConfig.java              # 资源客户端配置
│   │   └── TokenSecurityConfig.java               # Security 配置
│   │
│   ├── authentication/                            # 认证组件
│   │   ├── TokenAuthenticationProvider.java       # Token 认证提供者
│   │   └── TokenAuthenticationEntryPoint.java     # 认证入口点 (401 处理)
│   │
│   ├── filter/                                    # 过滤器
│   │   └── TokenAuthenticationFilter.java         # Token 认证过滤器
│   │       ├── 从 Header/Cookie/Parameter 提取 Token
│   │       ├── 调用 AuthenticationManager 验证
│   │       └── 设置 SecurityContext
│   │
│   └── aware/                                     # 自动注入
│       └── CurrentUserAutoAware.java              # 当前用户自动注入
│
├── src/main/resources/
│   └── META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── pom.xml
```

**核心流程**:
1. `TokenAuthenticationFilter` 提取 Token (Header > Cookie > Parameter)
2. `TokenAuthenticationProvider` 验证 Token (从 TokenRegistry 查询)
3. 设置 `SecurityContext`
4. 失败时 `TokenAuthenticationEntryPoint` 返回 401

---

### 2.7 imaping-token-test

**职责**: Token 系统测试应用

```
imaping-token-test/
├── src/main/java/imaping/token/test/
│   ├── TokenTestApplication.java                  # Spring Boot 启动类
│   ├── config/                                    # 配置类
│   │   └── TestSecurityConfigurerAdapter.java     # 测试安全配置
│   └── web/                                       # Web 控制器
│       ├── LoginController.java                   # 登录控制器
│       └── TestController.java                    # 测试控制器
│
├── src/main/resources/
│   └── application.properties                     # 应用配置
│
└── pom.xml
```

**核心特性**:
- 使用 Undertow 作为嵌入式服务器 (替代默认的 Tomcat)
- 集成 `imaping-token-redis-registry` 和 `imaping-token-resource-client`
- 提供 Token 认证的完整示例和测试接口
- 配置 `maven-deploy-plugin` 和 `maven-install-plugin` 跳过部署

**说明**: 此模块仅用于本地测试,不会被部署到 Maven 仓库

---

## 3. 包命名约定

### 3.1 包命名规则

```
com.imaping.token.<module>.<功能>
```

**示例**:
- `com.imaping.token.api.model` - Token 模型
- `com.imaping.token.api.registry` - Token 注册表
- `com.imaping.token.core.util` - 工具类

### 3.2 标准包结构

| 包名 | 用途 | 示例 |
|------|------|------|
| `model` | 数据模型、实体类 | `Token`, `BaseUserInfo` |
| `config` | 配置类 | `TokenApiConfig`, `TokenSecurityConfig` |
| `registry` | 注册表实现 | `TokenRegistry`, `DefaultTokenRegistry` |
| `factory` | 工厂类 | `TokenFactory`, `TimeoutTokenFactory` |
| `authentication` | 认证组件 | `Authentication`, `TokenAuthenticationProvider` |
| `filter` | 过滤器 | `TokenAuthenticationFilter` |
| `expiration` | 过期策略 | `ExpirationPolicy`, `TimeoutExpirationPolicy` |
| `generator` | 生成器 | `UniqueTokenIdGenerator` |
| `exception` | 异常类 | `TokenException`, `TokenNotFoundException` |
| `util` | 工具类 | `SecurityContextUtil`, `FunctionUtils` |
| `common` | 通用组件 | `BeanSupplier`, `Cleanable` |

---

## 4. 资源文件结构

### 4.1 Spring Boot 自动配置

**位置**: `src/main/resources/META-INF/spring/`

```
META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    ├── com.imaping.token.api.config.TokenApiConfig
    ├── com.imaping.token.core.TokenCoreAutoConfig
    ├── com.imaping.token.redis.registry.config.TokenConfig
    └── com.imaping.token.resource.client.config.ResourceClientConfig
```

### 4.2 配置元数据

**位置**: `src/main/resources/META-INF/`

```
META-INF/
└── spring-configuration-metadata.json             # 配置属性元数据 (IDE 提示)
```

### 4.3 测试资源

```
src/test/resources/
├── application-test.yml                           # 测试配置
└── logback-test.xml                              # 测试日志配置
```

---

## 5. 编译顺序和依赖关系

### 5.1 Maven Reactor 构建顺序

```
1. imaping-token-dependencies          (依赖管理)
   └── 无依赖

2. imaping-configuration-model         (配置模型)
   └── 依赖: Spring Boot, Jackson

3. imaping-token-core                  (核心模型)
   └── 依赖: Spring Boot, Spring Security

4. imaping-token-api                   (核心 API)
   └── 依赖: imaping-token-core, imaping-configuration-model

5. imaping-token-redis-registry        (Redis 实现)
   └── 依赖: imaping-token-api, Spring Data Redis

6. imaping-token-resource-client       (资源客户端)
   └── 依赖: imaping-token-api, Spring Security

7. imaping-token-test                  (测试应用)
   └── 依赖: imaping-token-redis-registry, imaping-token-resource-client, Spring Boot Web
```

### 5.2 模块依赖图

```
imaping-token-parent
│
├── imaping-token-dependencies
│   └── [无代码依赖]
│
├── imaping-configuration-model
│   └── [无内部依赖]
│
├── imaping-token-core
│   └── [无内部依赖]
│
├── imaping-token-api
│   ├── ← imaping-token-core
│   └── ← imaping-configuration-model
│
├── imaping-token-redis-registry
│   └── ← imaping-token-api
│
├── imaping-token-resource-client
│   └── ← imaping-token-api
│
└── imaping-token-test
    ├── ← imaping-token-redis-registry
    └── ← imaping-token-resource-client
```

---

## 6. 关键文件位置速查

### 6.1 配置文件

| 文件 | 位置 | 用途 |
|------|------|------|
| **核心配置类** | `imaping-configuration-model/.../IMapingConfigurationProperties.java` | 主配置类 |
| **Token 配置类** | `imaping-configuration-model/.../TokenConfigurationProperties.java` | Token 配置 |
| **API 自动配置** | `imaping-token-api/.../config/TokenApiConfig.java` | API 自动配置 |
| **Security 配置** | `imaping-token-resource-client/.../TokenSecurityConfig.java` | Security 配置 |

### 6.2 核心接口

| 接口 | 位置 | 用途 |
|------|------|------|
| **Token** | `imaping-token-api/.../model/Token.java` | Token 基础接口 |
| **TokenRegistry** | `imaping-token-api/.../registry/TokenRegistry.java` | 注册表接口 |
| **TokenFactory** | `imaping-token-api/.../factory/TokenFactory.java` | 工厂接口 |
| **ExpirationPolicy** | `imaping-token-api/.../expiration/ExpirationPolicy.java` | 过期策略接口 |

### 6.3 核心实现

| 实现类 | 位置 | 用途 |
|--------|------|------|
| **DefaultTokenRegistry** | `imaping-token-api/.../registry/DefaultTokenRegistry.java` | 内存注册表 |
| **RedisTokenRegistry** | `imaping-token-redis-registry/.../RedisTokenRegistry.java` | Redis 注册表 |
| **TokenAuthenticationFilter** | `imaping-token-resource-client/.../filter/TokenAuthenticationFilter.java` | Token 认证过滤器 |

---

## 7. 测试结构

### 7.1 测试目录组织

```
src/test/java/com/imaping/token/<module>/
├── unit/                                          # 单元测试
│   ├── model/
│   ├── registry/
│   └── factory/
├── integration/                                   # 集成测试
│   ├── TokenRegistryIntegrationTest.java
│   └── SecurityIntegrationTest.java
└── performance/                                   # 性能测试
    └── TokenRegistryPerformanceTest.java
```

### 7.2 测试命名约定

- 单元测试: `<ClassName>Test.java`
- 集成测试: `<Feature>IntegrationTest.java`
- 性能测试: `<Feature>PerformanceTest.java`

---

## 8. 常见操作指南

### 8.1 添加新的 Token 类型

**步骤 1**: 在 `imaping-token-api/src/main/java/com/imaping/token/api/model/` 创建接口和实现

```java
// 1. 定义接口
public interface RefreshToken extends Token { }

// 2. 实现类
public class DefaultRefreshToken extends AbstractToken implements RefreshToken { }
```

**步骤 2**: 在 `imaping-token-api/src/main/java/com/imaping/token/api/factory/` 创建工厂

```java
public class RefreshTokenFactory implements TokenFactory { }
```

### 8.2 添加新的存储后端

**步骤 1**: 在 `imaping-token-api/src/main/java/com/imaping/token/api/registry/` 或新模块中实现

```java
public class DatabaseTokenRegistry extends AbstractTokenRegistry { }
```

**步骤 2**: 在 `config/` 包中创建自动配置

```java
@Configuration
@ConditionalOnProperty("imaping.token.registry.database.enabled")
public class DatabaseTokenRegistryConfig {
    @Bean
    public TokenRegistry databaseTokenRegistry() { }
}
```

### 8.3 扩展认证过滤器

**位置**: `imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/filter/`

```java
public class CustomTokenAuthenticationFilter extends TokenAuthenticationFilter {
    // 重写 doFilterInternal 方法
}
```

---

## 9. 文档位置

### 9.1 项目文档

| 文档 | 位置 | 描述 |
|------|------|------|
| **架构文档** | `docs/architecture.md` | 完整架构设计文档 |
| **编码规范** | `docs/architecture/coding-standards.md` | 开发者必读编码标准 |
| **技术栈** | `docs/architecture/tech-stack.md` | 技术选型和工具链 |
| **源码结构** | `docs/architecture/source-tree.md` | 本文档 |
| **PRD** | `docs/prd.md` | 产品需求文档 |

### 9.2 代码文档

- **Javadoc**: 生成路径 `target/site/apidocs/`
- **生成命令**: `mvn javadoc:javadoc`

---

## 10. 快速导航

### 10.1 我要实现 Token 相关功能

→ 前往 `imaping-token-api/src/main/java/com/imaping/token/api/`

### 10.2 我要配置 Spring Security

→ 前往 `imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/config/`

### 10.3 我要添加配置属性

→ 前往 `imaping-configuration-model/src/main/java/com/imaping/token/configuration/model/`

### 10.4 我要实现存储后端

→ 前往 `imaping-token-api/src/main/java/com/imaping/token/api/registry/`
→ 或创建新模块 (参考 `imaping-token-redis-registry`)

---

**维护责任**: 架构团队
**更新频率**: 源码结构变更时更新
**审核流程**: 技术委员会批准

**相关文档**:
- [编码规范](coding-standards.md)
- [技术栈](tech-stack.md)
- [架构文档](../architecture.md)
