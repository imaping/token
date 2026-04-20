# 发布到 Maven Central

本文档说明如何将当前工程发布到 `central.sonatype.com` 的 `io.github.imaping` 命名空间。

## 1. 前置条件

1. 在 Sonatype Central Portal 中完成 `io.github.imaping` namespace 验证。
2. 在 Portal 中生成发布 Token。
3. 准备可用的 GPG 私钥、公钥和口令。
4. 确保当前版本号不是 `-SNAPSHOT`。

## 2. 本地配置

### 2.1 Maven `settings.xml`

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>你的 Central Token Username</username>
      <password>你的 Central Token Password</password>
    </server>
  </servers>
</settings>
```

### 2.2 GPG

确认本机已经导入可用密钥:

```bash
gpg --list-secret-keys --keyid-format LONG
```

## 3. 本地验证

不上传 Central，只验证打包链路:

```bash
mvn -P release-central -DskipTests -Dgpg.skip=true verify
```

如果你想连 `deploy` 阶段也完整走一遍，但不真正上传，可使用:

```bash
mvn -P release-central -DskipTests -Dgpg.skip=true -Dcentral.skipPublishing=true deploy
```

## 4. 本地发布

执行正式发布:

```bash
set MAVEN_GPG_PASSPHRASE=你的口令
mvn -P release-central -DskipTests -pl '!imaping-token-test' -am deploy
```

说明:

- `release-central` 会自动附加 `sources`、`javadocs` 并执行 GPG 签名。
- `imaping-token-test` 会在 Maven reactor 层直接排除，不参与签名和上传。
- 发布完成后，插件会等待组件进入 `published` 状态。
- 不建议使用 `-Dgpg.passphrase=...`，因为该方式已被 Maven GPG Plugin 标记为不推荐。

## 5. GitHub Actions Secrets

仓库中的 [publish-to-central.yml](../.github/workflows/publish-to-central.yml) 需要以下 Secrets:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## 6. 仍需人工确认的内容

- 开发者邮箱是否需要替换为你最终对外公开的邮箱。
- 版本号是否需要在发布前提升。
- Sonatype Portal 中的 namespace 是否已经完成验证。



