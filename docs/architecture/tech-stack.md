# 技术栈

> **快速参考**: imaping-token 项目使用的技术和工具链
> **最后更新**: 2025-10-12
> **项目版本**: 0.0.1-SNAPSHOT

---

## 1. 核心技术

| 技术 | 版本 | 用途 | 文档链接 |
|------|------|------|----------|
| **Java** | 17 | 编程语言 | [Oracle JDK 17](https://docs.oracle.com/en/java/javase/17/) |
| **Spring Boot** | 3.5.6 | 应用框架 | [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/3.5.6/reference/) |
| **Spring Security** | 6.x | 安全框架 | [Spring Security Docs](https://docs.spring.io/spring-security/reference/) |
| **Spring Data Redis** | 3.x | Redis 集成 | [Spring Data Redis Docs](https://docs.spring.io/spring-data/redis/reference/) |
| **Jackson** | 2.x | JSON 序列化 | [Jackson Docs](https://github.com/FasterXML/jackson-docs) |
| **Maven** | 3.x | 构建工具 | [Maven Docs](https://maven.apache.org/guides/) |

---

## 2. 第三方库

### 2.1 工具库

| 库 | 版本 | 用途 | 关键功能 |
|-----|------|------|----------|
| **Caffeine** | Latest | 内存缓存 | 高性能本地缓存实现 |
| **Lombok** | Latest | 代码简化 | 减少样板代码 (@Getter, @Setter, @Builder) |
| **Commons Lang3** | Latest | 工具类 | 字符串、数组、集合工具 |
| **Commons Codec** | Latest | 编码工具 | Base64、Hex 编码 |
| **Commons IO** | 2.11.0 | IO 工具 | 文件、流操作工具 |
| **Jool** | 0.9.14 | 函数式工具 | 增强的函数式编程支持 |

### 2.2 测试库

| 库 | 版本 | 用途 |
|-----|------|------|
| **JUnit 5** | 5.x | 单元测试框架 |
| **Mockito** | Latest | Mock 框架 |
| **AssertJ** | Latest | 流畅断言库 |
| **Spring Boot Test** | 3.5.6 | Spring 集成测试 |

---

## 3. 存储后端

### 3.1 存储选型对比

| 后端 | 使用场景 | 优势 | 劣势 | 配置开关 |
|------|---------|------|------|----------|
| **内存 (ConcurrentHashMap)** | 单机应用、开发环境 | • 快速<br>• 无外部依赖 | • 无持久化<br>• 无集群支持<br>• 重启丢失 | `imaping.token.registry.redis.enabled=false` |
| **Redis** | 分布式应用、生产环境 | • 持久化<br>• 集群共享<br>• TTL 自动过期<br>• 高可用 | • 外部依赖<br>• 网络延迟 | `imaping.token.registry.redis.enabled=true` |

### 3.2 Redis 特性支持

- ✅ **数据结构**: String (存储序列化的 Token 对象)
- ✅ **过期策略**: TTL 自动过期
- ✅ **持久化**: RDB + AOF
- ✅ **集群**: 支持主从复制、哨兵、Cluster
- ✅ **连接池**: Lettuce 连接池

---

## 4. 运行环境

### 4.1 开发环境要求

| 组件 | 最低版本 | 推荐版本 | 备注 |
|------|---------|---------|------|
| **JDK** | 17 | 17+ | Oracle JDK 或 OpenJDK |
| **Maven** | 3.6.0 | 3.8.x+ | 构建工具 |
| **IDE** | - | IntelliJ IDEA 2023+ | 推荐 Ultimate 版 |
| **Redis** | 6.0 | 7.x+ | 可选,用于分布式部署 |

### 4.2 生产环境要求

| 组件 | 最低版本 | 推荐配置 |
|------|---------|---------|
| **JVM** | OpenJDK 17 | `-Xmx2g -Xms2g -XX:+UseG1GC` |
| **应用服务器** | - | 嵌入式 Tomcat (Spring Boot 默认) |
| **Redis** | 6.0+ | 集群模式 (3 Master + 3 Slave) |
| **操作系统** | Linux 4.x+ | CentOS 8+ / Ubuntu 20.04+ |

---

## 5. 框架集成

### 5.1 Spring Boot Starters

项目使用的 Spring Boot Starters:

```xml
<!-- 核心 Web 框架 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Redis 集成 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 配置处理器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!-- 测试 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 5.2 Spring 核心组件

| 组件 | 版本 | 使用模块 | 用途 |
|------|------|---------|------|
| **spring-tx** | 6.x | imaping-token-api | 事务管理 |
| **spring-integration-core** | 6.x | imaping-token-api | 企业集成模式 |
| **spring-security-web** | 6.x | imaping-token-core, resource-client | Web 安全 |
| **spring-security-config** | 6.x | imaping-token-resource-client | Security 配置 |

---

## 6. 构建和依赖管理

### 6.1 Maven 多模块结构

```
imaping-token (父 POM)
├── imaping-token-dependencies      (依赖管理 BOM)
├── imaping-configuration-model     (配置模型)
├── imaping-token-core              (核心模型)
├── imaping-token-api               (核心 API)
├── imaping-token-redis-registry    (Redis 实现)
└── imaping-token-resource-client   (资源客户端)
```

### 6.2 版本管理策略

**统一版本管理** (`imaping-token-dependencies/pom.xml`):
- 继承 `spring-boot-dependencies:3.5.6`
- 使用 `${revision}` 变量统一子模块版本
- 通过 `<dependencyManagement>` 管理所有依赖版本

**版本更新流程**:
1. 更新 `imaping-token-dependencies` 的依赖版本
2. 子模块无需修改 (自动继承)
3. 减少版本冲突风险

### 6.3 编译配置

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

---

## 7. 开发工具链

### 7.1 推荐 IDE 插件

#### IntelliJ IDEA
- **Lombok Plugin** - Lombok 注解支持
- **Spring Assistant** - Spring Boot 配置提示
- **CheckStyle-IDEA** - 代码风格检查
- **SonarLint** - 代码质量检查

#### VS Code
- **Extension Pack for Java** - Java 开发支持
- **Spring Boot Extension Pack** - Spring Boot 开发支持

### 7.2 代码质量工具

| 工具 | 用途 | 配置文件 |
|------|------|---------|
| **Checkstyle** | 代码风格检查 | `checkstyle.xml` |
| **SpotBugs** | 静态代码分析 | `spotbugs-exclude.xml` |
| **PMD** | 代码质量检查 | `pmd-ruleset.xml` |
| **JaCoCo** | 测试覆盖率 | `pom.xml` (Maven Plugin) |

### 7.3 版本控制

- **Git** - 版本控制系统
- **Gitflow** - 分支管理策略
  - `main` - 生产环境分支
  - `develop` - 开发分支
  - `feature/*` - 功能分支
  - `release/*` - 发布分支
  - `hotfix/*` - 热修复分支

---

## 8. 性能和监控

### 8.1 性能工具

| 工具 | 用途 | 集成方式 |
|------|------|---------|
| **JMH** | 微基准测试 | Maven 依赖 |
| **JMeter** | 压力测试 | 外部工具 |
| **VisualVM** | JVM 监控 | 外部工具 |

### 8.2 监控组件 (可选集成)

| 组件 | 用途 | Spring Boot Starter |
|------|------|---------------------|
| **Spring Boot Actuator** | 应用监控端点 | `spring-boot-starter-actuator` |
| **Micrometer** | 指标收集 | 内置于 Actuator |
| **Prometheus** | 指标存储 | `micrometer-registry-prometheus` |
| **Grafana** | 指标可视化 | 外部工具 |

---

## 9. 安全工具

### 9.1 依赖安全扫描

| 工具 | 用途 | 使用方式 |
|------|------|---------|
| **OWASP Dependency Check** | 依赖漏洞扫描 | Maven Plugin |
| **Snyk** | 依赖和代码安全扫描 | CI/CD 集成 |

### 9.2 代码安全

- **SpotBugs Security Plugin** - 安全漏洞检测
- **FindSecBugs** - 安全最佳实践检查

---

## 10. 文档工具

### 10.1 API 文档

| 工具 | 用途 | 配置 |
|------|------|------|
| **Javadoc** | Java API 文档 | Maven Plugin |
| **SpringDoc OpenAPI** | REST API 文档 | `springdoc-openapi-starter-webmvc-ui` (可选) |

### 10.2 架构文档

- **Markdown** - 项目文档格式
- **PlantUML** - UML 图表 (可选)
- **Mermaid** - 流程图和架构图 (可选)

---

## 11. CI/CD 工具链 (推荐)

### 11.1 持续集成

| 工具 | 用途 | 配置文件 |
|------|------|---------|
| **GitHub Actions** | CI/CD 流水线 | `.github/workflows/*.yml` |
| **Jenkins** | CI/CD 服务器 | `Jenkinsfile` |
| **GitLab CI** | CI/CD 流水线 | `.gitlab-ci.yml` |

### 11.2 容器化 (可选)

| 工具 | 版本 | 用途 |
|------|------|------|
| **Docker** | 20.x+ | 容器化部署 |
| **Docker Compose** | 2.x+ | 本地环境编排 |
| **Kubernetes** | 1.25+ | 生产环境编排 |

---

## 12. 快速开始命令

### 12.1 编译项目

```bash
# 编译所有模块
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests

# 仅编译特定模块
mvn clean install -pl imaping-token-api -am
```

### 12.2 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl imaping-token-api

# 生成测试覆盖率报告
mvn jacoco:report
```

### 12.3 生成文档

```bash
# 生成 Javadoc
mvn javadoc:javadoc

# 查看 Javadoc
open target/site/apidocs/index.html
```

---

## 13. 环境配置示例

### 13.1 application.yml (开发环境)

```yaml
spring:
  profiles:
    active: dev

imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: false              # 开发环境使用内存存储
      inMemory:
        cache: true
        initialCapacity: 1000
    accessToken:
      timeToKillInSeconds: 7200     # 2小时
    scheduling:
      enabled: true
      repeatInterval: 120000        # 2分钟清理一次
```

### 13.2 application-prod.yml (生产环境)

```yaml
spring:
  profiles:
    active: prod
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

imaping:
  token:
    accessTokenName: access_token
    registry:
      redis:
        enabled: true               # 生产环境使用 Redis
      core:
        enable-locking: true        # 启用分布式锁
    accessToken:
      timeToKillInSeconds: 7200     # 2小时
    scheduling:
      enabled: false                # Redis 自动过期,无需定时清理
```

---

## 14. 技术选型决策

### 14.1 为什么选择 Java 17?
- ✅ LTS 版本,长期支持至 2029 年
- ✅ 新特性: Records, Sealed Classes, Pattern Matching
- ✅ 性能提升: G1GC 优化、ZGC 改进

### 14.2 为什么选择 Spring Boot 3.5.6?
- ✅ 最新稳定版,Bug 修复和性能优化
- ✅ 原生支持 Java 17+
- ✅ 完整的生态系统和社区支持

### 14.3 为什么选择 Redis 而非数据库?
- ✅ **性能**: 内存存储,微秒级响应
- ✅ **TTL 支持**: 原生自动过期机制
- ✅ **简单**: 无需设计表结构
- ✅ **分布式**: 天然支持集群

详见: [架构文档 - 9.4 为什么使用 Redis 而非数据库](architecture.md#94-为什么使用-redis-而非数据库)

### 14.4 为什么选择 Caffeine 而非 Guava Cache?
- ✅ **性能**: 读写吞吐量更高
- ✅ **算法**: W-TinyLFU 算法,更高的命中率
- ✅ **Spring 支持**: Spring Boot 默认缓存实现

---

## 15. 技术路线图

### 15.1 当前版本 (v0.0.1)
- ✅ 核心 Token 管理
- ✅ 内存和 Redis 存储
- ✅ Spring Security 集成
- ✅ 两种过期策略

### 15.2 计划特性 (v0.1.0)
- [ ] Refresh Token 支持
- [ ] OAuth2 集成
- [ ] 数据库存储实现 (MySQL/PostgreSQL)
- [ ] Metrics 和监控端点

### 15.3 未来考虑 (v1.0.0)
- [ ] 多租户支持
- [ ] 分布式追踪 (OpenTelemetry)
- [ ] Kubernetes Operator
- [ ] GraphQL API 支持

---

**维护责任**: 架构团队
**更新频率**: 技术栈变更时更新
**审核流程**: 技术委员会批准

**相关文档**:
- [编码规范](coding-standards.md)
- [源码结构](source-tree.md)
- [架构文档](../architecture.md)
