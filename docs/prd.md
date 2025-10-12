# imaping-token 系统改进和文档化 PRD

## 变更日志

| Change | Date | Version | Description | Author |
|--------|------|---------|-------------|--------|
| 初始创建 | 2025-10-11 | v1.0 | 创建PRD，定义Bug修复和文档化范围 | PM Agent |

---

## 1. 项目分析和上下文

### 1.1 分析来源
- **IDE-based新鲜分析** - 项目已在IDE中加载，基于实际代码分析

### 1.2 现有项目状态

**项目名称:** imaping-token

**技术栈:** Spring Boot 3.5.6 + Java 17 + Maven

**当前功能:**
imaping-token是一个企业级Token管理和认证系统，提供：
- Token生命周期管理（创建、存储、检索、更新、删除）
- 多种Token类型支持（HardTimeoutToken, TimeoutAccessToken）
- 灵活的过期策略（ExpirationPolicy）
- 多种存储后端（Redis、内存）
- 完整的认证机制（Authentication, Principal）
- 用户信息上下文管理

**模块结构:**
- `imaping-token-dependencies` - 依赖管理模块
- `imaping-configuration-model` - 配置属性模型
- `imaping-token-core` - 核心用户信息模型
- `imaping-token-api` - Token API核心逻辑
- `imaping-token-redis-registry` - Redis存储实现
- `imaping-token-resource-client` - 资源服务客户端

### 1.3 文档分析

**当前状态:** 项目缺少系统性文档
- ❌ 使用说明和快速入门指南
- ❌ API文档和配置参考
- ❌ 架构文档
- ❌ 最佳实践指南

### 1.4 增强范围定义

**增强类型:**
- ✅ Bug修复和稳定性改进
- ✅ 文档编写（使用说明）

**增强描述:**
本次增强旨在改善imaping-token系统的稳定性、兼容性和可用性：
1. **修复过期API和代码模式** - 升级到Spring Boot 3.x/Spring Security 6.x推荐的现代化写法
2. **创建完整的使用文档** - 降低用户学习曲线，加速系统接入

**影响评估:**
- ☑️ **最小影响** - 文档添加部分
- ☑️ **中等影响** - Bug修复涉及部分现有代码变更，但不改变外部API

### 1.5 目标

1. **提高代码现代化程度** - 消除过期API和模式，符合Spring Boot 3.x最佳实践
2. **增强系统兼容性** - 确保与最新Spring生态完全兼容
3. **改善开发者体验** - 提供清晰、全面的使用文档
4. **降低学习成本** - 通过快速入门指南加速用户上手

### 1.6 背景上下文

imaping-token已经实现了核心的Token管理功能，但在代码现代化和文档化方面存在改进空间：

**代码现代化问题:**
- 项目使用Spring Boot 3.5.6，但部分代码仍使用Spring 5.x时代的过期API
- 自动配置机制使用旧的`spring.factories`方式，而非Spring Boot 2.7+推荐的新机制
- 大量使用传统的`serialVersionUID`声明方式

**文档化问题:**
- 新用户难以快速理解系统架构和使用方式
- 缺少配置参考和最佳实践指导
- 接入成本较高

本次增强将系统化解决这些问题，为后续功能扩展奠定坚实基础。

---

## 2. 需求

### 2.1 功能性需求

#### FR1: 修复TokenSecurityConfig中的过期Spring Security API
**描述:** 将`TokenSecurityConfig`类中使用的Spring Security 5.x过期方法升级到6.x推荐API
- 替换 `antMatchers()` → `requestMatchers()`
- 替换 `mvcMatchers()` → `requestMatchers()`
- 确保配置语义和功能保持一致
- 验证所有安全规则仍正常工作

**位置:** `imaping-token-resource-client/src/main/java/com/imaping/token/resource/client/config/TokenSecurityConfig.java`

**优先级:** 高 - 影响系统在Spring Security 6.x下的正常运行

---

#### FR2: 迁移spring.factories到新的自动配置机制
**描述:** 将所有模块的`META-INF/spring.factories`迁移到Spring Boot 2.7+推荐的`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`格式

**涉及模块:**
- `imaping-token-core`
- `imaping-token-api`
- `imaping-token-redis-registry`
- `imaping-token-resource-client`
- `imaping-configuration-model`

**要求:**
- 保持所有自动配置功能不变
- 确保向后兼容性
- 验证各模块能正常自动装配

**优先级:** 中 - 提升现代化程度，为未来Spring Boot版本升级做准备

---

#### FR3: 优化serialVersionUID使用方式
**描述:** 审查39个包含`serialVersionUID`的类，根据实际需要优化：
- 对于不需要跨版本序列化兼容的类，移除显式声明
- 对于需要的类，考虑使用Java 14+ `@Serial`注解
- 保持序列化协议兼容性

**涉及范围:**
- `imaping-token-api` (16个类)
- `imaping-configuration-model` (19个类)
- `imaping-token-core` (3个类)
- 其他模块 (1个类)

**优先级:** 低 - 代码整洁度改进，不影响功能

---

#### FR4: 创建系统使用文档
**描述:** 创建完整的系统使用文档，包含以下内容：

**4.1 快速入门指南 (Quick Start)**
- 系统简介和核心概念
- Maven依赖配置
- 最简配置示例
- 5分钟运行示例

**4.2 架构概览 (Architecture Overview)**
- 模块结构说明
- 核心组件介绍
- Token生命周期流程图
- 扩展点说明

**4.3 配置参考 (Configuration Reference)**
- 所有配置项详细说明
- 配置项分类（Token、Redis、安全等）
- 默认值和推荐配置
- 配置示例

**4.4 API使用指南 (API Guide)**
- TokenRegistry API使用
- TokenFactory使用
- 自定义Token类型
- 自定义过期策略

**4.5 集成指南 (Integration Guide)**
- Spring Security集成
- Redis配置
- 自定义存储后端
- 多实例部署

**4.6 最佳实践 (Best Practices)**
- Token过期策略选择
- 性能优化建议
- 安全性建议
- 故障排查指南

**输出格式:** Markdown文档，存放在`docs/`目录

**优先级:** 高 - 直接影响用户体验

---

### 2.2 非功能性需求

#### NFR1: 向后兼容性
**描述:** 所有Bug修复必须保持API和配置的向后兼容性
- 外部API签名不变
- 配置属性不变
- 序列化格式兼容（对于需要的类）

**验证方式:**
- 单元测试通过
- 集成测试通过
- 手动验证现有配置仍可用

---

#### NFR2: 代码质量
**描述:** 修改后的代码必须符合现代Java和Spring最佳实践
- 使用推荐的API和模式
- 添加必要的注释
- 保持代码风格一致

---

#### NFR3: 文档质量
**描述:** 文档必须清晰、准确、易读
- 使用清晰的结构和标题层次
- 包含实际可运行的代码示例
- 提供配置说明和参数表格
- 中英文双语支持（优先中文）

---

#### NFR4: 测试覆盖
**描述:** Bug修复必须有对应的测试验证
- 单元测试覆盖修改的代码
- 集成测试验证端到端功能
- 回归测试确保无破坏性变更

---

### 2.3 兼容性需求

#### CR1: Spring Boot版本兼容
**要求:** 确保与Spring Boot 3.5.6完全兼容，并为未来3.x版本升级做准备

#### CR2: Java版本兼容
**要求:** 保持Java 17作为最低版本要求

#### CR3: 依赖兼容性
**要求:** 确保与现有依赖（Redis、Lombok等）兼容

#### CR4: 部署兼容性
**要求:** 修改不影响现有部署方式和运维流程

---

## 3. 技术约束和集成要求

### 3.1 现有技术栈

**语言:** Java 17
**框架:** Spring Boot 3.5.6, Spring Security 6.x
**构建工具:** Maven
**数据库/存储:** Redis (支持), 内存存储
**依赖管理:** 统一版本管理通过 `imaping-token-dependencies`
**关键依赖:** Lombok, Jackson, jOOQ Lambda, Apache Commons Lang3

### 3.2 集成方法

**Bug修复集成策略:**
- 最小化代码变更范围
- 优先使用Spring Boot推荐的迁移路径
- 保持现有测试通过

**文档集成策略:**
- 文档存放在`docs/`目录
- 使用Markdown格式，便于版本控制和协作
- 在README.md中添加文档索引链接

### 3.3 代码组织和标准

**文件结构:**
```
docs/
  ├── README.md              # 文档索引
  ├── quick-start.md         # 快速入门
  ├── architecture.md        # 架构概览
  ├── configuration.md       # 配置参考
  ├── api-guide.md          # API使用指南
  ├── integration.md        # 集成指南
  └── best-practices.md     # 最佳实践
```

**命名约定:**
- 文档文件使用kebab-case命名
- 保持与现有代码风格一致

**编码标准:**
- 遵循Spring Boot官方迁移指南
- 使用IDE代码格式化（统一配置）
- 添加必要的JavaDoc注释

### 3.4 部署和运维

**构建过程集成:**
- 无需修改现有Maven构建配置
- 文档可选择性打包到发布版本

**部署策略:**
- Bug修复通过常规发版流程
- 文档可独立更新

**监控和日志:**
- 保持现有日志级别和格式
- 不引入新的监控依赖

**配置管理:**
- 保持现有配置属性结构
- 在文档中说明所有可配置项

### 3.5 风险评估和缓解

**技术风险:**
- **风险:** Spring Security API变更可能导致安全配置行为差异
- **缓解:** 充分的集成测试，逐步验证每个安全规则

**集成风险:**
- **风险:** 自动配置迁移可能导致Bean加载顺序变化
- **缓解:** 保留原有`spring.factories`作为过渡，逐步验证

**部署风险:**
- **风险:** 序列化格式变更可能导致已存储Token无法反序列化
- **缓解:** 仅移除不影响序列化的`serialVersionUID`，关键类保留

**缓解策略:**
- 分阶段实施（先修复高优先级问题）
- 充分的测试覆盖
- 提供回滚方案
- 文档明确说明变更内容

---

## 4. Epic和Story结构

### 4.1 Epic方法论

**Epic结构决策:** 采用**单一综合Epic**的方式

**理由:**

- 所有改进都围绕"系统现代化和可用性"这一核心目标
- 技术域一致，不涉及新功能模块
- 依赖关系紧密，文档需要基于修复后的代码编写
- 风险可控，变更范围明确
- 适合逐步完成和验证

---

## Epic 1: imaping-token系统现代化和文档化

**Epic目标:**
升级imaping-token系统中的过期API和代码模式，使其完全符合Spring Boot 3.x最佳实践，并创建完整的用户使用文档，提升系统的现代化程度和可用性。

**集成要求:**
- 保持外部API向后兼容
- 确保现有测试全部通过
- 文档与代码同步更新
- 增量提交，便于代码审查

---

### Story 1.1: 修复TokenSecurityConfig中的Spring Security过期API

**用户故事:**

作为系统维护者，
我希望TokenSecurityConfig使用Spring Security 6.x推荐的API，
以便系统能够与最新的Spring Security版本完全兼容，避免未来的弃用警告和潜在问题。

**验收标准:**

1. 所有`antMatchers()`调用替换为`requestMatchers()`
2. 所有`mvcMatchers()`调用替换为`requestMatchers()`
3. `.securityMatchers().requestMatchers().antMatchers()`链式调用修正为正确的Spring Security 6.x语法
4. 保持所有安全规则语义不变（authenticated路径、permit路径、HTTP方法规则）
5. 现有单元测试和集成测试全部通过
6. 代码能够正常编译，无弃用警告
7. 手动验证：启动应用，测试认证和授权功能正常

**集成验证:**

- IV1: 验证原有的认证流程（Token认证、Bearer Token）仍正常工作
- IV2: 验证所有路径访问规则（permit、authenticated）与修改前行为一致
- IV3: 验证HTTP方法级别的安全规则（GET、POST等）正确生效

---

### Story 1.2: 迁移spring.factories到新的自动配置机制

**用户故事:**

作为系统维护者，
我希望使用Spring Boot 2.7+推荐的自动配置导入机制，
以便系统符合现代Spring Boot标准，为未来版本升级做好准备。

**验收标准:**

1. 在每个模块的`src/main/resources/META-INF/spring/`目录下创建`org.springframework.boot.autoconfigure.AutoConfiguration.imports`文件
2. 将原`spring.factories`中的`org.springframework.boot.autoconfigure.EnableAutoConfiguration`配置项迁移到新文件
3. 保留`spring.factories`中的其他配置项（如果有）
4. 验证5个模块的自动配置类都能正常加载：
   - `imaping-token-core` → `TokenCoreAutoConfig`
   - `imaping-token-api` → `TokenApiConfig`, `TokenSchedulingConfiguration`
   - `imaping-token-redis-registry` → Redis相关配置
   - `imaping-token-resource-client` → `ResourceClientConfig`, `TokenSecurityConfig`
   - `imaping-configuration-model` → 配置属性类
5. 应用能够正常启动，所有Bean正确注入
6. 自动配置的条件注解（`@ConditionalOnXXX`）仍然生效

**集成验证:**

- IV1: 验证TokenRegistry Bean能够正常自动装配
- IV2: 验证SecurityFilterChain正确创建和生效
- IV3: 验证配置属性（`IMapingConfigurationProperties`）正确加载

---

### Story 1.3: 审查和优化serialVersionUID使用

**用户故事:**

作为代码维护者，
我希望审查项目中的serialVersionUID使用，移除不必要的声明，
以便提升代码整洁度，同时保持必要的序列化兼容性。

**验收标准:**

1. 审查39个包含`serialVersionUID`的类，分类处理：
   - **需要保留的类**（跨JVM传输、持久化存储）：Token相关模型类、Authentication类、Exception类
   - **可以移除的类**：纯配置类（Properties）、仅在内存使用的类
2. 对于保留serialVersionUID的类，验证其确实需要序列化兼容性
3. 对于移除serialVersionUID的类，确认不影响现有功能
4. 添加注释说明为什么保留serialVersionUID（对于保留的类）
5. 确保Redis存储的Token对象能够正常序列化/反序列化
6. 所有测试通过

**集成验证:**

- IV1: 验证Token对象在Redis中的存储和读取不受影响
- IV2: 验证Exception在分布式环境中的传递不受影响
- IV3: 验证配置属性对象的序列化（如需要）不受影响

---

### Story 1.4: 创建快速入门和架构文档

**用户故事:**

作为新用户，
我希望有清晰的快速入门指南和架构文档，
以便能够在30分钟内理解系统并运行第一个示例。

**验收标准:**

1. 创建`docs/quick-start.md`，包含：
   - 系统简介（3-5句话描述imaping-token是什么）
   - 核心概念（Token、TokenRegistry、ExpirationPolicy）
   - Maven依赖配置示例
   - 最简配置示例（application.yml）
   - 5分钟运行示例（完整可运行代码）
2. 创建`docs/architecture.md`，包含：
   - 6个模块的功能说明和依赖关系
   - 核心组件介绍（用类图或文字描述）
   - Token生命周期流程图（文字描述或Mermaid图）
   - 扩展点说明（如何自定义Token类型、存储后端、过期策略）
3. 创建`docs/README.md`作为文档索引
4. 所有代码示例经过验证，可以直接运行
5. 使用中文编写，保持术语一致性

**集成验证:**

- IV1: 按照快速入门文档，能够在全新环境中成功运行示例
- IV2: 架构文档准确反映实际代码结构
- IV3: 文档链接和格式在GitHub/GitLab上正确渲染

---

### Story 1.5: 创建配置参考和API使用指南

**用户故事:**

作为系统集成者，
我希望有完整的配置参考和API使用指南，
以便能够根据实际需求正确配置和使用系统。

**验收标准:**

1. 创建`docs/configuration.md`，包含：
   - 所有配置项的完整列表（从`IMapingConfigurationProperties`及其嵌套类提取）
   - 配置项分类（Token配置、Redis配置、安全配置、调度配置等）
   - 每个配置项的说明、类型、默认值、示例
   - 常见配置场景示例（开发环境、生产环境、集群部署）
2. 创建`docs/api-guide.md`，包含：
   - TokenRegistry API使用（addToken、getToken、deleteToken、updateToken）
   - TokenFactory使用（创建不同类型的Token）
   - 自定义Token类型的步骤和示例
   - 自定义过期策略的步骤和示例
   - 所有示例代码可运行
3. 使用表格展示配置项，便于查找
4. 代码示例包含完整的import语句和错误处理

**集成验证:**

- IV1: 配置参考中的所有配置项与实际代码一致
- IV2: API使用示例能够成功编译和运行
- IV3: 自定义扩展示例能够正确集成到系统

---

### Story 1.6: 创建集成指南和最佳实践文档

**用户故事:**

作为系统架构师，
我希望有集成指南和最佳实践文档，
以便能够在生产环境中正确部署和优化系统。

**验收标准:**

1. 创建`docs/integration.md`，包含：
   - Spring Security集成详细步骤（如何在现有Spring Security项目中集成）
   - Redis配置和连接池优化
   - 内存存储与Redis存储的选择建议
   - 多实例部署配置（分布式场景）
   - 与Spring Boot Actuator集成（健康检查、指标监控）
2. 创建`docs/best-practices.md`，包含：
   - Token过期策略选择指南（HardTimeout vs Timeout）
   - 性能优化建议（批量操作、缓存策略）
   - 安全性建议（Token长度、密钥管理、HTTPS）
   - 故障排查指南（常见错误和解决方案）
   - 监控和告警建议
3. 包含实际的配置示例和代码片段
4. 提供性能参考数据（如适用）

**集成验证:**

- IV1: 集成指南能够指导真实的系统集成
- IV2: 最佳实践建议基于实际代码能力和限制
- IV3: 故障排查指南覆盖常见问题

---

## 5. 实施计划

### 5.1 Story执行顺序

按照以下顺序执行Story，确保依赖关系和风险最小化：

1. **Story 1.1** (高优先级) - 修复Spring Security API
2. **Story 1.2** (中优先级) - 迁移自动配置机制
3. **Story 1.3** (低优先级) - 优化serialVersionUID
4. **Story 1.4** (高优先级) - 创建基础文档
5. **Story 1.5** (高优先级) - 创建配置和API文档
6. **Story 1.6** (高优先级) - 创建高级文档

**理由:**

- Story 1.1-1.3 为代码修复，优先完成确保代码基础稳定
- Story 1.4-1.6 为文档编写，基于修复后的代码编写，按照用户学习路径排序

### 5.2 验收检查清单

每个Story完成后必须通过以下检查：

- [ ] 所有验收标准已满足
- [ ] 所有集成验证已通过
- [ ] 单元测试通过（代码修复Story）
- [ ] 集成测试通过（代码修复Story）
- [ ] 代码审查完成（代码修复Story）
- [ ] 文档审查完成（文档Story）
- [ ] 示例代码已验证可运行（文档Story）
- [ ] 无回归问题

### 5.3 成功标准

Epic整体成功标准：

1. **代码现代化:**
   - 无Spring Security弃用警告
   - 使用新的自动配置机制
   - 代码整洁度提升

2. **文档完整性:**
   - 新用户能在30分钟内上手
   - 配置参考覆盖所有配置项
   - API指南涵盖常见使用场景
   - 最佳实践指导生产环境部署

3. **质量保证:**
   - 所有测试通过
   - 无破坏性变更
   - 向后兼容

4. **用户反馈:**
   - 文档清晰易懂
   - 示例可直接使用
   - 降低接入成本

---

## 6. 附录

### 6.1 参考资料

- [Spring Security 6.x Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Spring Boot 3.x Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Spring Boot AutoConfiguration Registration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

### 6.2 术语表

| 术语 | 说明 |
|------|------|
| Token | 访问令牌，用于标识和验证用户会话 |
| TokenRegistry | Token注册表，负责Token的存储和管理 |
| ExpirationPolicy | 过期策略，定义Token何时过期 |
| HardTimeoutToken | 硬超时Token，从创建时刻开始计时 |
| TimeoutAccessToken | 滑动超时Token，从最后使用时刻开始计时 |
| Principal | 主体，表示已认证的用户身份 |

### 6.3 联系方式

如有问题，请联系：

- 项目负责人：[待补充]
- 技术支持：[待补充]

---

**文档版本:** v1.0
**最后更新:** 2025-10-11
**状态:** 待审批

