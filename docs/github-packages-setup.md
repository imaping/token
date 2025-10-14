# GitHub Packages 自动发布配置

## 概述

本项目已配置自动发布到 GitHub Packages。仅在创建版本标签时自动构建并发布 Maven 包。

## 工作流触发条件

自动发布会在以下情况触发:

1. **版本标签**: 推送符合 `v*.*.*` 格式的标签(例如: `v1.0.0`, `v2.1.3`)
2. **手动触发**: 在 GitHub Actions 页面手动运行

⚠️ **注意**: 推送代码到分支不会触发自动发布,只有打标签才会发布。

## 配置说明

### 1. POM 配置

已在父 [pom.xml](../pom.xml:55-61) 中添加了 `distributionManagement` 配置:

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/${env.GITHUB_REPOSITORY}</url>
    </repository>
</distributionManagement>
```

### 2. GitHub Actions 工作流

工作流文件位于 [.github/workflows/publish-to-github-packages.yml](../.github/workflows/publish-to-github-packages.yml)

**工作流步骤**:
1. 检出代码
2. 设置 JDK 17 环境
3. 创建 Maven settings.xml 配置文件(自动注入 GitHub Token)
4. 构建项目 (`mvn clean verify`)
5. 发布到 GitHub Packages (`mvn deploy`)

### 3. 权限配置

工作流使用内置的 `GITHUB_TOKEN`,具有以下权限:
- `contents: read` - 读取仓库内容
- `packages: write` - 写入包到 GitHub Packages

**无需额外配置**,GitHub 会自动提供这个 token。

## 使用发布的包

### 在其他项目中引用

1. **配置 Maven settings.xml**

在 `~/.m2/settings.xml` 中添加:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

2. **获取 GitHub Token**

访问: https://github.com/settings/tokens
- 创建 Personal Access Token
- 勾选 `read:packages` 权限

3. **在项目中添加仓库配置**

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/YOUR_USERNAME/REPO_NAME</url>
    </repository>
</repositories>
```

4. **添加依赖**

```xml
<dependency>
    <groupId>com.imaping</groupId>
    <artifactId>imaping-token-core</artifactId>
    <version>0.0.6-SNAPSHOT</version>
</dependency>
```

## 版本管理

### 快照版本(SNAPSHOT)

当前版本: `0.0.6-SNAPSHOT`

- 自动发布到 GitHub Packages
- 每次构建都会覆盖
- 适用于开发和测试

### 正式版本

发布正式版本的步骤:

1. **更新版本号**

修改 [pom.xml](../pom.xml:31) 中的 `revision` 属性:

```xml
<revision>1.0.0</revision>
```

2. **创建并推送 Git 标签**

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

推送标签后,GitHub Actions 会自动触发构建和发布流程。

## 手动触发发布

1. 访问 GitHub 仓库的 Actions 页面
2. 选择 "Publish to GitHub Packages" 工作流
3. 点击 "Run workflow"
4. 选择要运行的分支
5. 点击 "Run workflow" 按钮

## 查看发布的包

访问仓库的 Packages 页面:
```
https://github.com/YOUR_USERNAME/REPO_NAME/packages
```

## 故障排查

### 发布失败

1. **检查权限**: 确保仓库设置中启用了 GitHub Packages
2. **查看日志**: 在 Actions 页面查看详细的构建日志
3. **验证 POM**: 确保所有模块都能正常编译

### 无法下载包

1. **检查 Token 权限**: 确保 PAT 包含 `read:packages`
2. **验证仓库 URL**: 检查 repository URL 是否正确
3. **认证配置**: 确保 settings.xml 配置正确

## 模块说明

项目包含以下可发布的模块:

- `imaping-token-dependencies` - 依赖管理
- `imaping-token-configuration-model` - 配置模型
- `imaping-token-core` - 核心功能
- `imaping-token-api` - API 接口
- `imaping-token-redis-registry` - Redis 注册实现
- `imaping-token-resource-client` - 资源客户端
- `imaping-token-test` - 测试模块

所有模块都会自动发布到 GitHub Packages。

## 最佳实践

1. **版本命名**:
   - 开发版本使用 SNAPSHOT
   - 正式版本使用语义化版本(如 1.0.0)

2. **分支策略**:
   - `main`/`master` 用于稳定版本
   - 功能分支开发完成后合并

3. **发布频率**:
   - SNAPSHOT 版本可以频繁发布
   - 正式版本建议在稳定后发布

4. **文档同步**:
   - 每次发布前更新 CHANGELOG
   - 在 Release notes 中说明变更内容

## 相关链接

- [GitHub Packages 文档](https://docs.github.com/en/packages)
- [Maven 配置指南](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
