# imaping-token 文档中心

> **企业级 Token 管理和认证系统**
>
> 版本: 0.0.6-SNAPSHOT | Spring Boot 3.5.6 | Java 17

---

## 📚 文档导航

### 🚀 快速开始

- **[快速入门指南](quick-start.md)** ⭐ 推荐新用户从这里开始
  - 5 分钟快速体验
  - Maven 依赖配置
  - 最简配置示例
  - Hello World 示例

### 🏗️ 架构设计

- **[架构文档](architecture.md)** 📐
  - 系统概述和技术栈
  - 模块架构和职责
  - 核心组件详解
  - 数据流与交互
  - 部署架构
  - 安全架构
  - 扩展点说明
  - 技术决策记录 (ADR)

- **[架构图表集](architecture-diagrams.md)** 📊
  - 模块依赖图
  - Token 类型层次结构
  - TokenRegistry 实现层次
  - Token 创建/验证流程
  - 自动配置加载顺序
  - 部署架构图

### ⚙️ 配置和使用

- **[配置参考](configuration.md)** 🔧 ⭐ 新增
  - 完整配置项列表
  - 配置项分类和说明
  - 常见配置场景示例
  - 开发/生产环境配置

- **[API 使用指南](api-guide.md)** 💻 ⭐ 新增
  - TokenRegistry API
  - TokenFactory 使用
  - 自定义 Token 类型
  - 自定义过期策略

### 🔌 集成和部署

- **[集成指南](integration.md)** 🔗 ⭐ 新增
  - Spring Security 集成详细步骤
  - Redis 配置和连接池优化
  - 存储选择建议
  - 多实例部署配置
  - Spring Boot Actuator 集成
  - 常见集成场景示例

- **[最佳实践](best-practices.md)** ⚡ ⭐ 新增
  - Token 过期策略选择指南
  - 性能优化建议
  - 安全性建议
  - 故障排查指南
  - 监控和告警建议
  - 开发和测试最佳实践

- **[故障排查](troubleshooting.md)** 🔍 (待创建)
  - 常见问题和解决方案
  - 日志分析
  - 性能问题排查

---

## 🎯 根据场景选择文档

### 我是新用户,想快速上手

1. ✅ [快速入门指南](quick-start.md) - 了解系统并运行第一个示例
2. ✅ [架构文档](architecture.md) - 理解系统设计和核心概念
3. ✅ [配置参考](configuration.md) - 配置系统参数
4. ✅ [API 使用指南](api-guide.md) - 学习如何使用 API

### 我需要在生产环境部署

1. ✅ [架构文档 - 部署架构](architecture.md#6-部署架构) - 了解部署模式
2. ✅ [配置参考](configuration.md) - 生产环境配置
3. ✅ [集成指南](integration.md) - Redis 配置和集群部署
4. ✅ [最佳实践](best-practices.md) - 生产环境优化建议

### 我想自定义扩展功能

1. ✅ [架构文档 - 扩展点](architecture.md#8-扩展点) - 了解扩展机制
2. ✅ [架构文档 - 核心组件](architecture.md#4-核心组件) - 理解组件设计
3. ✅ [API 使用指南](api-guide.md) - 自定义实现示例

### 我遇到了问题

1. ✅ [最佳实践 - 故障排查指南](best-practices.md#4-故障排查指南) - 常见问题解决方案
2. ✅ [架构文档](architecture.md) - 理解系统运行原理
3. 📧 联系支持团队

---

## 📖 文档状态

| 文档 | 状态 | 最后更新 | 优先级 |
|------|------|----------|--------|
| [架构文档](architecture.md) | ✅ 已完成 | 2025-10-12 | 高 |
| [快速入门指南](quick-start.md) | ✅ 已完成 | 2025-10-12 | 高 |
| [架构图表集](architecture-diagrams.md) | ✅ 已完成 | 2025-10-11 | 高 |
| [配置参考](configuration.md) | ✅ 已完成 | 2025-10-12 | 高 |
| [API 使用指南](api-guide.md) | ✅ 已完成 | 2025-10-12 | 中 |
| [集成指南](integration.md) | ✅ 已完成 | 2025-10-12 | 高 |
| [最佳实践](best-practices.md) | ✅ 已完成 | 2025-10-12 | 高 |
| [故障排查](troubleshooting.md) | ⬜ 待创建 | - | 低 |

**图例**:
- ✅ 已完成
- 🚧 进行中
- ⬜ 待创建

---

## 🏗️ 系统概览

### 核心能力

```
┌─────────────────────────────────────────────────────────────┐
│                     imaping-token 系统                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ Token 生命周期管理 (创建、存储、检索、更新、删除)          │
│  ✅ 多种 Token 类型 (TimeoutAccessToken, HardTimeoutToken)  │
│  ✅ 灵活的过期策略 (自动续期、固定时间)                       │
│  ✅ 多存储后端 (内存、Redis)                                 │
│  ✅ Spring Security 深度集成                                 │
│  ✅ 分布式会话管理                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 模块结构

```
imaping-token-parent
├── imaping-token-dependencies         # 依赖管理
├── imaping-configuration-model        # 配置模型
├── imaping-token-core                 # 核心用户上下文
├── imaping-token-api                  # Token API 核心
├── imaping-token-redis-registry       # Redis 存储实现
└── imaping-token-resource-client      # Spring Security 集成
```

详见: [架构文档 - 模块架构](architecture.md#3-模块架构)

### 技术栈

- **Java**: 17
- **Spring Boot**: 3.5.6
- **Spring Security**: 6.x
- **Redis**: Spring Data Redis
- **构建工具**: Maven

详见: [架构文档 - 技术栈](architecture.md#2-技术栈)

---

## 🔗 外部资源

### 相关项目

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)

### 推荐阅读

- [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/index.html)
- [Redis 最佳实践](https://redis.io/docs/manual/patterns/)

---

## 📝 文档贡献

### 文档维护

- **维护责任**: 架构团队
- **更新频率**: 每次重大变更
- **审核流程**: 技术评审委员会批准

### 如何贡献

1. Fork 项目
2. 创建文档分支 (`git checkout -b docs/feature-name`)
3. 提交变更 (`git commit -m 'docs: add new section'`)
4. 推送到分支 (`git push origin docs/feature-name`)
5. 创建 Pull Request

### 文档规范

- 使用 Markdown 格式
- 遵循 [中文文案排版指北](https://github.com/sparanoid/chinese-copywriting-guidelines)
- 代码示例必须可运行
- 图表使用 Mermaid 语法

---

## 📄 许可证

本项目采用 [Apache License 2.0](../LICENSE) 许可证。

---

## 📧 联系我们

- **问题反馈**: [GitHub Issues](https://github.com/your-org/imaping-token/issues)
- **技术支持**: support@example.com
- **文档问题**: docs@example.com

---

**最后更新**: 2025-10-12
**文档版本**: v4



