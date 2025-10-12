# 编码规范

> **快速参考**: 开发者必读的编码标准和最佳实践
> **最后更新**: 2025-10-12
> **适用范围**: imaping-token 项目所有模块

---

## 1. 核心设计原则

### 1.1 DRY (Don't Repeat Yourself)
- ✅ 使用抽象基类封装通用逻辑 (`AbstractToken`, `AbstractTokenRegistry`)
- ✅ 配置统一管理 (`IMapingConfigurationProperties`)
- ❌ 避免重复的代码块和相似的实现

### 1.2 KISS (Keep It Simple)
- ✅ 接口定义简洁明了 (`Token`, `TokenRegistry`)
- ✅ 方法职责单一,命名清晰
- ✅ 默认实现开箱即用
- ❌ 避免过度工程和不必要的复杂性

### 1.3 SOLID 原则

#### 单一职责原则 (S)
- 每个类专注于一个明确的职责
- 示例: `TokenFactory` 只负责创建 Token,不处理存储

#### 开闭原则 (O)
- 通过接口和抽象类支持扩展
- 示例: 通过实现 `TokenRegistry` 接口扩展新的存储后端

#### 里氏替换原则 (L)
- Token 层次结构可替换
- 示例: `DefaultTimeoutAccessToken` 可以替换为任何 `TimeoutAccessToken` 实现

#### 接口隔离原则 (I)
- 小而专一的接口设计
- 示例: `TimeoutAccessToken` 和 `HardTimeoutToken` 分离

#### 依赖倒置原则 (D)
- 依赖抽象而非具体实现
- 示例: 依赖 `TokenRegistry` 接口,而非 `DefaultTokenRegistry`

### 1.4 YAGNI (You Aren't Gonna Need It)
- ✅ 仅实现当前需要的功能
- ❌ 避免预先设计未来可能需要的特性

---

## 2. Java 编码规范

### 2.1 命名约定

#### 类名
- 接口: 清晰的名词 (`Token`, `TokenRegistry`, `ExpirationPolicy`)
- 抽象类: `Abstract` 前缀 (`AbstractToken`, `AbstractTokenRegistry`)
- 实现类: `Default` 前缀或描述性名称 (`DefaultTokenFactory`, `RedisTokenRegistry`)
- 配置类: `Config` 或 `Configuration` 后缀 (`TokenApiConfig`, `IMapingPropertiesConfiguration`)

#### 方法名
- 动词开头: `createToken()`, `deleteToken()`, `isExpired()`
- 布尔方法: `is/has/can` 前缀 (`isExpired()`, `hasToken()`)
- 获取方法: `get` 前缀 (`getToken()`, `getExpirationPolicy()`)

#### 变量名
- 驼峰命名: `tokenRegistry`, `expirationPolicy`
- 常量: 全大写蛇形 (`PREFIX`, `MAX_RETRY_COUNT`)
- 避免单字母变量 (除循环索引外)

### 2.2 包结构规范

标准包结构:
```
com.imaping.token.<module>
├── model/             # 数据模型
├── registry/          # 注册表实现
├── factory/           # 工厂类
├── expiration/        # 过期策略
├── authentication/    # 认证组件
├── generator/         # 生成器
├── config/            # 配置类
├── exception/         # 异常定义
└── util/              # 工具类
```

### 2.3 类结构顺序

```java
public class Example {
    // 1. 静态常量
    private static final String PREFIX = "EX";

    // 2. 成员变量 (按访问权限: private -> protected -> public)
    private String id;
    protected ExpirationPolicy policy;

    // 3. 构造函数
    public Example() { }
    public Example(String id) { this.id = id; }

    // 4. 静态方法
    public static Example create() { }

    // 5. 公共方法
    @Override
    public String getId() { return id; }

    // 6. 保护方法
    protected void validate() { }

    // 7. 私有方法
    private void init() { }

    // 8. 内部类/枚举
    private static class Builder { }
}
```

### 2.4 注解使用

#### 必需注解
```java
@Component          // Spring 组件
@Service            // 服务层
@Repository         // 数据访问层
@Configuration      // 配置类
@Bean               // Bean 定义
@Override           // 重写方法 (必须)
@Deprecated         // 废弃的方法/类
```

#### 条件装配
```java
@ConditionalOnProperty(name = "imaping.token.registry.redis.enabled", havingValue = "true")
@ConditionalOnMissingBean(TokenRegistry.class)
@AutoConfigureBefore(TokenApiConfig.class)
```

#### Lombok 注解 (谨慎使用)
```java
@Getter             // 生成 getter
@Setter             // 生成 setter
@ToString           // 生成 toString
@EqualsAndHashCode  // 生成 equals 和 hashCode
@Builder            // 生成建造者模式
@Slf4j              // 生成日志对象
```

⚠️ **避免使用**: `@Data` (过于宽泛)、`@AllArgsConstructor` (破坏封装)

### 2.5 异常处理

#### 异常层次
```java
// 基础异常
public class TokenException extends RuntimeException { }

// 具体异常
public class TokenNotFoundException extends TokenException { }
public class TokenExpiredException extends TokenException { }
```

#### 异常处理原则
- ✅ 捕获具体异常,而非 `Exception`
- ✅ 记录异常日志: `log.error("Token not found: {}", tokenId, e)`
- ✅ 提供上下文信息
- ❌ 避免空 catch 块
- ❌ 避免捕获后重新抛出相同异常

```java
// ✅ 好的实践
try {
    Token token = tokenRegistry.getToken(tokenId);
    if (token == null) {
        throw new TokenNotFoundException("Token not found: " + tokenId);
    }
} catch (TokenNotFoundException e) {
    log.error("Failed to retrieve token: {}", tokenId, e);
    throw e;
}

// ❌ 避免
try {
    // ...
} catch (Exception e) {  // 太宽泛
    throw e;             // 无意义的重新抛出
}
```

---

## 3. Spring Boot 规范

### 3.1 自动配置

#### 配置类结构
```java
@Configuration
@AutoConfigureBefore(NextConfig.class)
@ConditionalOnProperty(name = "imaping.token.enabled", havingValue = "true", matchIfMissing = true)
public class TokenApiConfig {

    @Bean
    @ConditionalOnMissingBean
    public TokenRegistry tokenRegistry() {
        return new DefaultTokenRegistry();
    }
}
```

#### 配置顺序
1. `IMapingPropertiesConfiguration` - 加载配置属性
2. `TokenConfig` - Redis 配置 (如果启用)
3. `TokenApiConfig` - 核心 API 配置
4. `TokenCoreAutoConfig` - 核心模型配置
5. `TokenSchedulingConfiguration` - 调度配置
6. `ResourceClientConfig` - Security 配置

#### spring.factories / AutoConfiguration.imports
```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.imaping.token.api.config.TokenApiConfig
com.imaping.token.core.TokenCoreAutoConfig
```

### 3.2 配置属性

#### 使用 @ConfigurationProperties
```java
@ConfigurationProperties(prefix = "imaping.token")
@Data
public class TokenConfigurationProperties {
    private String accessTokenName = "access_token";
    private TokenRegistryProperties registry = new TokenRegistryProperties();
    private AccessTokenProperties accessToken = new AccessTokenProperties();
}
```

#### 配置元数据
在 `src/main/resources/META-INF/spring-configuration-metadata.json` 提供配置提示

### 3.3 依赖注入

#### 构造函数注入 (推荐)
```java
@Service
public class TokenService {
    private final TokenRegistry tokenRegistry;
    private final TokenFactory tokenFactory;

    // 单构造函数可省略 @Autowired
    public TokenService(TokenRegistry tokenRegistry, TokenFactory tokenFactory) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
    }
}
```

#### 字段注入 (避免)
```java
// ❌ 避免使用字段注入
@Autowired
private TokenRegistry tokenRegistry;
```

---

## 4. 测试规范

### 4.1 测试结构

```
src/test/java
└── com/imaping/token/<module>
    ├── integration/        # 集成测试
    ├── unit/              # 单元测试
    └── performance/       # 性能测试
```

### 4.2 单元测试

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
        Authentication auth = createAuthentication();
        Token expectedToken = createToken();
        when(tokenRegistry.addToken(any())).thenReturn(expectedToken);

        // When
        Token actualToken = tokenService.createToken(auth);

        // Then
        assertThat(actualToken).isNotNull();
        assertThat(actualToken.getId()).isEqualTo(expectedToken.getId());
        verify(tokenRegistry, times(1)).addToken(any());
    }
}
```

### 4.3 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class TokenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 4.4 测试覆盖率要求

- 核心业务逻辑: ≥ 80%
- 工具类/通用组件: ≥ 90%
- 配置类: ≥ 60%

---

## 5. 日志规范

### 5.1 日志级别

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 系统错误、异常 | `log.error("Failed to connect to Redis", e)` |
| **WARN** | 警告、可恢复的错误 | `log.warn("Token {} expired, cleaning up", tokenId)` |
| **INFO** | 重要业务事件 | `log.info("Token created: {}", tokenId)` |
| **DEBUG** | 调试信息 | `log.debug("Checking token expiration: {}", token)` |
| **TRACE** | 详细跟踪信息 | `log.trace("Token details: {}", token.toString())` |

### 5.2 日志格式

```java
// ✅ 使用占位符,避免字符串拼接
log.info("Token created: {}, user: {}", tokenId, userId);

// ❌ 避免字符串拼接
log.info("Token created: " + tokenId + ", user: " + userId);

// ✅ 异常日志包含上下文
log.error("Failed to delete token: {}", tokenId, e);

// ❌ 避免泛泛的日志
log.error("Error occurred", e);
```

### 5.3 敏感信息

⚠️ **绝对不要记录敏感信息**:
- 密码
- Token 完整内容 (可记录前6位: `token.substring(0, 6)`)
- 用户完整手机号/邮箱 (可脱敏: `mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")`)

---

## 6. 文档规范

### 6.1 Javadoc

#### 类级别
```java
/**
 * Token 注册表的默认实现,使用 ConcurrentHashMap 作为存储.
 *
 * <p>适用于单机应用场景,提供快速的内存访问但不支持持久化和集群共享.</p>
 *
 * @author imaping-team
 * @since 0.0.1
 * @see TokenRegistry
 * @see AbstractTokenRegistry
 */
public class DefaultTokenRegistry extends AbstractMapBasedTokenRegistry {
}
```

#### 方法级别
```java
/**
 * 创建新的 Token 并添加到注册表.
 *
 * @param authentication 认证信息,不能为 null
 * @return 创建的 Token 实例
 * @throws IllegalArgumentException 如果 authentication 为 null
 * @throws TokenCreationException 如果创建失败
 */
public Token createToken(Authentication authentication) {
}
```

### 6.2 代码注释

```java
// ✅ 解释 "为什么" 而非 "是什么"
// 使用 ConcurrentHashMap 确保多线程安全,避免加锁开销
private final Map<String, Token> tokens = new ConcurrentHashMap<>();

// ❌ 避免显而易见的注释
// 创建 Map
private final Map<String, Token> tokens = new HashMap<>();
```

---

## 7. Maven 规范

### 7.1 依赖管理

#### 统一版本管理
在 `imaping-token-dependencies` 模块统一管理所有依赖版本:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 子模块继承
```xml
<parent>
    <groupId>com.imaping</groupId>
    <artifactId>imaping-token-dependencies</artifactId>
    <version>${revision}</version>
</parent>
```

### 7.2 编译顺序

Maven Reactor 构建顺序:
1. `imaping-token-dependencies`
2. `imaping-configuration-model`
3. `imaping-token-core`
4. `imaping-token-api`
5. `imaping-token-redis-registry`
6. `imaping-token-resource-client`

### 7.3 依赖原则

- ✅ 使用 `<scope>provided</scope>` for servlet-api
- ✅ 使用 `<optional>true</optional>` for 可选依赖
- ❌ 避免传递依赖冲突 (使用 `<exclusions>`)
- ❌ 避免循环依赖

---

## 8. 安全规范

### 8.1 Token 生成

```java
// ✅ 使用安全的随机生成器
UniqueTokenIdGenerator tokenIdGenerator = new DefaultUniqueTokenIdGenerator(
    new Base64RandomStringGenerator(),
    32  // 32字符长度 (256位熵)
);

// ❌ 避免使用可预测的生成器
// UUID.randomUUID().toString()  // 可预测性较高
```

### 8.2 过期时间

```yaml
imaping:
  token:
    accessToken:
      timeToKillInSeconds: 7200  # 2小时 (生产环境推荐)
```

- ✅ 短期 Token: 1-4 小时
- ❌ 避免无限期 Token

### 8.3 传输安全

- ✅ 生产环境强制 HTTPS
- ✅ Cookie 设置 `Secure` 和 `HttpOnly` 属性
- ✅ 使用 `SameSite=Strict` 防止 CSRF

---

## 9. 性能规范

### 9.1 缓存策略

```java
// ✅ 使用 Caffeine 缓存提升性能
@Bean
public TokenRegistry cachingTokenRegistry() {
    return new CachingTokenRegistry(
        Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofHours(2))
            .build()
    );
}
```

### 9.2 数据库查询优化

- ✅ 批量操作优于循环单条操作
- ✅ 使用索引加速查询
- ✅ 避免 N+1 查询问题

### 9.3 Redis 优化

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20    # 最大连接数
          max-idle: 10      # 最大空闲连接
          min-idle: 5       # 最小空闲连接
```

---

## 10. 代码审查清单

### 10.1 必查项

- [ ] 遵循 SOLID 原则
- [ ] 命名清晰有意义
- [ ] 异常处理完善
- [ ] 日志记录合理
- [ ] 单元测试覆盖核心逻辑
- [ ] 无安全漏洞 (SQL 注入、XSS 等)
- [ ] 无敏感信息泄露
- [ ] 性能考虑合理

### 10.2 推荐项

- [ ] Javadoc 完整
- [ ] 设计模式使用恰当
- [ ] 代码可读性良好
- [ ] 无重复代码
- [ ] 配置可外部化

---

## 11. 常见问题

### Q1: 什么时候使用接口,什么时候使用抽象类?

**A**:
- **接口**: 定义契约 (`Token`, `TokenRegistry`)
- **抽象类**: 封装通用实现 (`AbstractToken`, `AbstractTokenRegistry`)

### Q2: 如何选择 Redis 还是内存存储?

**A**:
- **单机应用**: 使用内存存储 (`DefaultTokenRegistry`)
- **分布式应用**: 使用 Redis (`RedisTokenRegistry`)

### Q3: 如何扩展新的 Token 类型?

**A**: 参见架构文档 [8.1 自定义 Token 类型](architecture.md#81-自定义-token-类型)

---

**维护责任**: 架构团队
**更新频率**: 每次编码规范变更
**审核流程**: 技术委员会批准
