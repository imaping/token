# API 使用指南

> **快速参考**: imaping-token API 使用方法和扩展指南
> **最后更新**: 2025-10-12
> **版本**: 0.0.6-SNAPSHOT

---

## 目录

- [1. API 概览](#1-api-概览)
- [2. TokenRegistry API 使用](#2-tokenregistry-api-使用)
  - [2.1 添加 Token (addToken)](#21-添加-token-addtoken)
  - [2.2 获取 Token (getToken)](#22-获取-token-gettoken)
  - [2.3 更新 Token (updateToken)](#23-更新-token-updatetoken)
  - [2.4 删除 Token (deleteToken)](#24-删除-token-deletetoken)
  - [2.5 统计方法](#25-统计方法)
- [3. TokenFactory 使用](#3-tokenfactory-使用)
  - [3.1 DefaultTokenFactory 使用](#31-defaulttokenfactory-使用)
  - [3.2 创建 TimeoutAccessToken (自动续期)](#32-创建-timeoutaccesstoken-自动续期)
  - [3.3 创建 HardTimeoutToken (固定时间)](#33-创建-hardtimeouttoken-固定时间)
  - [3.4 自定义 Token ID 生成器](#34-自定义-token-id-生成器)
- [4. 自定义 Token 类型](#4-自定义-token-类型)
  - [4.1 实现步骤](#41-实现步骤)
  - [4.2 完整示例: RefreshToken](#42-完整示例-refreshtoken)
- [5. 自定义过期策略](#5-自定义过期策略)
  - [5.1 实现步骤](#51-实现步骤)
  - [5.2 完整示例: CustomExpirationPolicy](#52-完整示例-customexpirationpolicy)
- [6. 最佳实践](#6-最佳实践)

---

## 1. API 概览

imaping-token 提供三个核心 API:

| API | 职责 | 常用方法 |
|-----|------|----------|
| **TokenRegistry** | Token 存储和管理 | `addToken()`, `getToken()`, `updateToken()`, `deleteToken()` |
| **TokenFactory** | Token 创建工厂 | `createToken()` (通过子类实现) |
| **ExpirationPolicy** | Token 过期策略 | `isExpired()`, `getTimeToLive()` |

### 核心依赖注入

在 Spring 应用中,使用构造函数注入获取 API 实例:

```java
package com.example.service;

import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.api.factory.TokenFactory;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final TokenRegistry tokenRegistry;
    private final TokenFactory tokenFactory;

    // 构造函数注入 (推荐)
    public TokenService(TokenRegistry tokenRegistry, TokenFactory tokenFactory) {
        this.tokenRegistry = tokenRegistry;
        this.tokenFactory = tokenFactory;
    }
}
```

---

## 2. TokenRegistry API 使用

`TokenRegistry` 是 Token 存储和管理的核心接口,提供 Token 的增删改查操作。

### 2.1 添加 Token (addToken)

**方法签名:**
```java
void addToken(Token token) throws Exception;
```

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.authentication.principal.Principal;
import com.imaping.token.api.factory.TimeoutTokenFactory;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.registry.TokenRegistry;
import com.imaping.token.core.model.BaseUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenRegistry tokenRegistry;
    private final TimeoutTokenFactory timeoutTokenFactory;

    /**
     * 创建并添加 Token 到注册表
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return Token ID
     */
    public String createAndAddToken(String userId, String username) {
        try {
            // 1. 创建用户信息
            BaseUserInfo userInfo = new BaseUserInfo();
            userInfo.setId(userId);
            userInfo.setUsername(username);

            // 2. 创建认证主体
            Principal principal = Principal.builder()
                    .id(userId)
                    .userInfo(userInfo)
                    .build();

            // 3. 创建认证信息
            Authentication authentication = Authentication.builder()
                    .principal(principal)
                    .build();

            // 4. 使用工厂创建 Token
            TimeoutAccessToken token = (TimeoutAccessToken) timeoutTokenFactory.createToken(authentication);

            // 5. 添加 Token 到注册表
            tokenRegistry.addToken(token);

            log.info("Token created and added successfully: {}", token.getId());
            return token.getId();

        } catch (Exception e) {
            log.error("Failed to create and add token for user: {}", userId, e);
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * 批量添加 Token
     *
     * @param tokens Token 流
     */
    public void addMultipleTokens(Stream<TimeoutAccessToken> tokens) {
        try {
            tokenRegistry.addToken(tokens);
            log.info("Multiple tokens added successfully");
        } catch (Exception e) {
            log.error("Failed to add multiple tokens", e);
            throw new RuntimeException("Batch token addition failed", e);
        }
    }
}
```

**关键点:**
- Token 必须先通过 `TokenFactory` 创建,再添加到注册表
- `addToken()` 会抛出受检异常,必须处理
- 支持批量添加 (使用 `Stream<Token>`)

---

### 2.2 获取 Token (getToken)

**方法签名:**
```java
Token getToken(String tokenId);
<T extends Token> T getToken(String tokenId, Class<T> clazz);
Token getToken(String tokenId, Predicate<Token> predicate);
```

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.model.Token;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.model.HardTimeoutToken;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenQueryService {

    private final TokenRegistry tokenRegistry;

    /**
     * 获取 Token (基础方法)
     *
     * @param tokenId Token ID
     * @return Token 实例,如果不存在则返回 null
     */
    public Token getToken(String tokenId) {
        Token token = tokenRegistry.getToken(tokenId);
        if (token == null) {
            log.warn("Token not found: {}", tokenId);
            return null;
        }
        log.debug("Token retrieved: {}", tokenId);
        return token;
    }

    /**
     * 获取指定类型的 Token (类型安全)
     *
     * @param tokenId Token ID
     * @return TimeoutAccessToken 实例
     * @throws ClassCastException 如果 Token 类型不匹配
     */
    public TimeoutAccessToken getTimeoutAccessToken(String tokenId) {
        TimeoutAccessToken token = tokenRegistry.getToken(tokenId, TimeoutAccessToken.class);
        if (token == null) {
            log.warn("TimeoutAccessToken not found: {}", tokenId);
            return null;
        }
        return token;
    }

    /**
     * 获取 HardTimeoutToken
     *
     * @param tokenId Token ID
     * @return HardTimeoutToken 实例
     */
    public HardTimeoutToken getHardTimeoutToken(String tokenId) {
        return tokenRegistry.getToken(tokenId, HardTimeoutToken.class);
    }

    /**
     * 使用 Predicate 获取 Token (条件过滤)
     *
     * @param tokenId Token ID
     * @return 未过期的 Token,如果过期或不存在则返回 null
     */
    public Token getValidToken(String tokenId) {
        Predicate<Token> notExpired = token -> !token.isExpired();
        Token token = tokenRegistry.getToken(tokenId, notExpired);
        if (token == null) {
            log.warn("Token not found or expired: {}", tokenId);
        }
        return token;
    }

    /**
     * 安全获取 Token (使用 Optional)
     *
     * @param tokenId Token ID
     * @return Optional<Token>
     */
    public Optional<Token> findToken(String tokenId) {
        return Optional.ofNullable(tokenRegistry.getToken(tokenId));
    }

    /**
     * 检查 Token 是否存在且有效
     *
     * @param tokenId Token ID
     * @return true 如果 Token 存在且未过期
     */
    public boolean isTokenValid(String tokenId) {
        Token token = tokenRegistry.getToken(tokenId);
        return token != null && !token.isExpired();
    }
}
```

**关键点:**
- `getToken(String)` 返回 `Token` 基类,需要手动类型转换
- `getToken(String, Class<T>)` 返回指定类型,类型安全
- `getToken(String, Predicate<Token>)` 支持条件过滤
- 返回 `null` 表示 Token 不存在或不符合条件

---

### 2.3 更新 Token (updateToken)

**方法签名:**
```java
Token updateToken(Token token) throws Exception;
```

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.model.Token;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUpdateService {

    private final TokenRegistry tokenRegistry;

    /**
     * 更新 Token (记录使用时间和次数)
     *
     * @param tokenId Token ID
     * @return 更新后的 Token
     */
    public Token updateToken(String tokenId) {
        try {
            // 1. 获取 Token
            Token token = tokenRegistry.getToken(tokenId);
            if (token == null) {
                log.warn("Token not found for update: {}", tokenId);
                return null;
            }

            // 2. 检查是否过期
            if (token.isExpired()) {
                log.warn("Attempting to update expired token: {}", tokenId);
                return null;
            }

            // 3. 更新 Token (记录使用时间和次数)
            token.update();

            // 4. 保存到注册表
            Token updatedToken = tokenRegistry.updateToken(token);

            log.info("Token updated successfully: {}, usage count: {}",
                    tokenId, updatedToken.getCountOfUses());

            return updatedToken;

        } catch (Exception e) {
            log.error("Failed to update token: {}", tokenId, e);
            throw new RuntimeException("Token update failed", e);
        }
    }

    /**
     * 刷新 TimeoutAccessToken (延长过期时间)
     *
     * @param tokenId Token ID
     * @return 刷新后的 Token
     */
    public TimeoutAccessToken refreshToken(String tokenId) {
        try {
            TimeoutAccessToken token = tokenRegistry.getToken(tokenId, TimeoutAccessToken.class);
            if (token == null) {
                log.warn("TimeoutAccessToken not found: {}", tokenId);
                return null;
            }

            // TimeoutAccessToken.update() 会自动刷新过期时间
            token.update();
            tokenRegistry.updateToken(token);

            log.info("Token refreshed: {}, last used: {}",
                    tokenId, token.getLastTimeUsed());

            return token;

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", tokenId, e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * 手动标记 Token 为过期
     *
     * @param tokenId Token ID
     */
    public void expireToken(String tokenId) {
        try {
            Token token = tokenRegistry.getToken(tokenId);
            if (token == null) {
                log.warn("Token not found for expiration: {}", tokenId);
                return;
            }

            // 标记为过期
            token.markTokenExpired();
            tokenRegistry.updateToken(token);

            log.info("Token marked as expired: {}", tokenId);

        } catch (Exception e) {
            log.error("Failed to expire token: {}", tokenId, e);
            throw new RuntimeException("Token expiration failed", e);
        }
    }
}
```

**关键点:**
- `token.update()` 会更新 `lastTimeUsed` 和 `countOfUses`
- `TimeoutAccessToken` 的 `update()` 会自动延长过期时间
- `HardTimeoutToken` 的 `update()` 不会延长过期时间
- 更新后必须调用 `tokenRegistry.updateToken()` 保存

---

### 2.4 删除 Token (deleteToken)

**方法签名:**
```java
int deleteToken(String tokenId) throws Exception;
int deleteToken(Token token) throws Exception;
long deleteAll();
```

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenDeletionService {

    private final TokenRegistry tokenRegistry;

    /**
     * 删除 Token (按 ID)
     *
     * @param tokenId Token ID
     * @return 删除的 Token 数量 (包括关联的子 Token)
     */
    public int deleteToken(String tokenId) {
        try {
            int deletedCount = tokenRegistry.deleteToken(tokenId);
            if (deletedCount > 0) {
                log.info("Token deleted: {}, count: {}", tokenId, deletedCount);
            } else {
                log.warn("Token not found for deletion: {}", tokenId);
            }
            return deletedCount;

        } catch (Exception e) {
            log.error("Failed to delete token: {}", tokenId, e);
            throw new RuntimeException("Token deletion failed", e);
        }
    }

    /**
     * 删除 Token (按对象)
     *
     * @param token Token 对象
     * @return 删除的 Token 数量
     */
    public int deleteToken(Token token) {
        try {
            int deletedCount = tokenRegistry.deleteToken(token);
            log.info("Token deleted: {}, count: {}", token.getId(), deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("Failed to delete token: {}", token.getId(), e);
            throw new RuntimeException("Token deletion failed", e);
        }
    }

    /**
     * 删除过期的 Token
     *
     * @return 删除的 Token 数量
     */
    public long deleteExpiredTokens() {
        try {
            long deletedCount = tokenRegistry.getTokens().stream()
                    .filter(Token::isExpired)
                    .mapToInt(token -> {
                        try {
                            return tokenRegistry.deleteToken(token);
                        } catch (Exception e) {
                            log.error("Failed to delete expired token: {}", token.getId(), e);
                            return 0;
                        }
                    })
                    .sum();

            log.info("Deleted {} expired tokens", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("Failed to delete expired tokens", e);
            throw new RuntimeException("Expired token deletion failed", e);
        }
    }

    /**
     * 删除所有 Token (谨慎使用!)
     *
     * @return 删除的 Token 数量
     */
    public long deleteAllTokens() {
        log.warn("Deleting all tokens - this is a destructive operation!");
        long deletedCount = tokenRegistry.deleteAll();
        log.info("All tokens deleted: {} tokens", deletedCount);
        return deletedCount;
    }

    /**
     * 删除指定用户的所有 Token
     *
     * @param principalId 用户 ID
     * @return 删除的 Token 数量
     */
    public long deleteTokensForUser(String principalId) {
        try {
            long deletedCount = tokenRegistry.getSessionsFor(principalId)
                    .mapToInt(token -> {
                        try {
                            return tokenRegistry.deleteToken(token);
                        } catch (Exception e) {
                            log.error("Failed to delete token for user: {}", principalId, e);
                            return 0;
                        }
                    })
                    .sum();

            log.info("Deleted {} tokens for user: {}", deletedCount, principalId);
            return deletedCount;

        } catch (Exception e) {
            log.error("Failed to delete tokens for user: {}", principalId, e);
            throw new RuntimeException("User token deletion failed", e);
        }
    }
}
```

**关键点:**
- 返回值表示删除的 Token 数量 (包括关联的子 Token)
- `deleteAll()` 是危险操作,生产环境慎用
- 删除不存在的 Token 返回 `0`
- 可以使用 Stream API 批量删除符合条件的 Token

---

### 2.5 统计方法

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenStatisticsService {

    private final TokenRegistry tokenRegistry;

    /**
     * 获取当前会话总数
     *
     * @return 会话数量
     */
    public long getSessionCount() {
        long count = tokenRegistry.sessionCount();
        log.debug("Total session count: {}", count);
        return count;
    }

    /**
     * 获取指定用户的会话数
     *
     * @param principalId 用户 ID
     * @return 用户的会话数量
     */
    public long getUserSessionCount(String principalId) {
        long count = tokenRegistry.countSessionsFor(principalId);
        log.debug("Session count for user {}: {}", principalId, count);
        return count;
    }

    /**
     * 获取指定用户的所有会话
     *
     * @param principalId 用户 ID
     * @return 用户的所有 Token
     */
    public List<Token> getUserSessions(String principalId) {
        List<Token> sessions = tokenRegistry.getSessionsFor(principalId)
                .collect(Collectors.toList());
        log.debug("User {} has {} active sessions", principalId, sessions.size());
        return sessions;
    }

    /**
     * 获取所有 Token
     *
     * @return 所有 Token 集合
     */
    public Collection<? extends Token> getAllTokens() {
        Collection<? extends Token> tokens = tokenRegistry.getTokens();
        log.debug("Total tokens: {}", tokens.size());
        return tokens;
    }

    /**
     * 获取所有有效的 Token (未过期)
     *
     * @return 有效 Token 列表
     */
    public List<Token> getValidTokens() {
        List<Token> validTokens = tokenRegistry.getTokens(token -> !token.isExpired())
                .collect(Collectors.toList());
        log.debug("Valid tokens: {}", validTokens.size());
        return validTokens;
    }

    /**
     * 检查用户是否有活动会话
     *
     * @param principalId 用户 ID
     * @return true 如果用户有至少一个活动会话
     */
    public boolean hasActiveSession(String principalId) {
        return tokenRegistry.countSessionsFor(principalId) > 0;
    }

    /**
     * 获取 Token 统计信息
     *
     * @return 统计信息对象
     */
    public TokenStatistics getStatistics() {
        long total = tokenRegistry.sessionCount();
        long valid = tokenRegistry.getTokens(token -> !token.isExpired()).count();
        long expired = total - valid;

        return TokenStatistics.builder()
                .totalTokens(total)
                .validTokens(valid)
                .expiredTokens(expired)
                .build();
    }

    /**
     * Token 统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class TokenStatistics {
        private long totalTokens;
        private long validTokens;
        private long expiredTokens;
    }
}
```

**关键点:**
- `sessionCount()` 返回总会话数
- `countSessionsFor(principalId)` 返回指定用户的会话数
- `getSessionsFor(principalId)` 返回指定用户的所有未过期 Token
- `getTokens(Predicate)` 支持条件过滤

---

## 3. TokenFactory 使用

`TokenFactory` 负责创建 Token 实例。

### 3.1 DefaultTokenFactory 使用

`DefaultTokenFactory` 是组合工厂,可以注册多个子工厂。

**完整示例:**

```java
package com.example.config;

import com.imaping.token.api.factory.DefaultTokenFactory;
import com.imaping.token.api.factory.TimeoutTokenFactory;
import com.imaping.token.api.factory.HardTimeoutTokenFactory;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.model.HardTimeoutToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenFactoryConfig {

    /**
     * 配置组合工厂
     */
    @Bean
    public DefaultTokenFactory defaultTokenFactory(
            TimeoutTokenFactory timeoutTokenFactory,
            HardTimeoutTokenFactory hardTimeoutTokenFactory) {

        DefaultTokenFactory factory = new DefaultTokenFactory();

        // 注册子工厂
        factory.addTokenFactory(TimeoutAccessToken.class, timeoutTokenFactory);
        factory.addTokenFactory(HardTimeoutToken.class, hardTimeoutTokenFactory);

        return factory;
    }
}
```

**使用组合工厂:**

```java
package com.example.service;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.factory.DefaultTokenFactory;
import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.model.HardTimeoutToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenCreationService {

    private final DefaultTokenFactory defaultTokenFactory;

    /**
     * 使用组合工厂创建不同类型的 Token
     */
    public TimeoutAccessToken createTimeoutToken(Authentication authentication) {
        // 获取 TimeoutTokenFactory
        TokenFactory factory = defaultTokenFactory.get(TimeoutAccessToken.class);
        return (TimeoutAccessToken) factory.createToken(authentication);
    }

    public HardTimeoutToken createHardTimeoutToken(Authentication authentication) {
        // 获取 HardTimeoutTokenFactory
        TokenFactory factory = defaultTokenFactory.get(HardTimeoutToken.class);
        return (HardTimeoutToken) factory.createToken(authentication);
    }
}
```

---

### 3.2 创建 TimeoutAccessToken (自动续期)

`TimeoutAccessToken` 每次使用都会自动刷新过期时间。

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.authentication.principal.Principal;
import com.imaping.token.api.factory.TimeoutTokenFactory;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.core.model.BaseUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeoutTokenService {

    private final TimeoutTokenFactory timeoutTokenFactory;

    /**
     * 创建自动续期 Token
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return TimeoutAccessToken
     */
    public TimeoutAccessToken createTimeoutToken(String userId, String username) {
        // 创建用户信息
        BaseUserInfo userInfo = new BaseUserInfo();
        userInfo.setId(userId);
        userInfo.setUsername(username);

        // 创建认证信息
        Principal principal = Principal.builder()
                .id(userId)
                .userInfo(userInfo)
                .build();

        Authentication authentication = Authentication.builder()
                .principal(principal)
                .build();

        // 创建 Token
        TimeoutAccessToken token = (TimeoutAccessToken) timeoutTokenFactory.createToken(authentication);

        log.info("TimeoutAccessToken created: {}, expires at: {}",
                token.getId(),
                token.getCreationTime().plus(token.getExpirationPolicy().getTimeToLive(), java.time.temporal.ChronoUnit.SECONDS));

        return token;
    }

    /**
     * 使用并刷新 Token (自动续期)
     *
     * @param token TimeoutAccessToken
     */
    public void useAndRefreshToken(TimeoutAccessToken token) {
        // update() 会自动刷新过期时间
        token.update();

        log.info("Token used and refreshed: {}, last used: {}, usage count: {}",
                token.getId(),
                token.getLastTimeUsed(),
                token.getCountOfUses());
    }
}
```

**特点:**
- 每次调用 `update()` 会刷新 `lastTimeUsed`,并重新计算过期时间
- 适用于需要"活跃续期"的场景 (如 Web 会话)
- Token 前缀: `AT-`

---

### 3.3 创建 HardTimeoutToken (固定时间)

`HardTimeoutToken` 从创建时间开始计算,过期时间固定不变。

**完整示例:**

```java
package com.example.service;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.authentication.principal.Principal;
import com.imaping.token.api.factory.HardTimeoutTokenFactory;
import com.imaping.token.api.model.HardTimeoutToken;
import com.imaping.token.core.model.BaseUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HardTimeoutTokenService {

    private final HardTimeoutTokenFactory hardTimeoutTokenFactory;

    /**
     * 创建固定时间 Token
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return HardTimeoutToken
     */
    public HardTimeoutToken createHardTimeoutToken(String userId, String username) {
        // 创建用户信息
        BaseUserInfo userInfo = new BaseUserInfo();
        userInfo.setId(userId);
        userInfo.setUsername(username);

        // 创建认证信息
        Principal principal = Principal.builder()
                .id(userId)
                .userInfo(userInfo)
                .build();

        Authentication authentication = Authentication.builder()
                .principal(principal)
                .build();

        // 创建 Token
        HardTimeoutToken token = (HardTimeoutToken) hardTimeoutTokenFactory.createToken(authentication);

        ZonedDateTime expirationTime = token.getCreationTime()
                .plusSeconds(token.getExpirationPolicy().getTimeToLive());

        log.info("HardTimeoutToken created: {}, will expire at: {}",
                token.getId(),
                expirationTime);

        return token;
    }

    /**
     * 使用 Token (不会延长过期时间)
     *
     * @param token HardTimeoutToken
     */
    public void useToken(HardTimeoutToken token) {
        // update() 只会更新使用时间和次数,不会延长过期时间
        token.update();

        log.info("Token used: {}, usage count: {}, expires at: {}",
                token.getId(),
                token.getCountOfUses(),
                token.getCreationTime().plusSeconds(token.getExpirationPolicy().getTimeToLive()));
    }

    /**
     * 检查 Token 是否即将过期
     *
     * @param token HardTimeoutToken
     * @param thresholdMinutes 阈值 (分钟)
     * @return true 如果在阈值时间内即将过期
     */
    public boolean isTokenExpiringSoon(HardTimeoutToken token, long thresholdMinutes) {
        ZonedDateTime now = ZonedDateTime.now(token.getExpirationPolicy().getClock());
        ZonedDateTime expirationTime = token.getCreationTime()
                .plusSeconds(token.getExpirationPolicy().getTimeToLive());

        long minutesUntilExpiration = java.time.Duration.between(now, expirationTime).toMinutes();

        return minutesUntilExpiration <= thresholdMinutes && minutesUntilExpiration > 0;
    }
}
```

**特点:**
- `update()` 不会延长过期时间
- 过期时间 = 创建时间 + TTL
- 适用于需要"绝对过期时间"的场景 (如验证码、临时链接)
- Token 前缀: `ATT-`

---

### 3.4 自定义 Token ID 生成器

**完整示例:**

```java
package com.example.generator;

import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import com.imaping.token.api.generator.RandomStringGenerator;
import com.imaping.token.api.generator.Base64RandomStringGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenIdGeneratorConfig {

    /**
     * 自定义 Token ID 生成器 (使用 Base64 编码,长度 32)
     */
    @Bean
    public UniqueTokenIdGenerator customTokenIdGenerator() {
        RandomStringGenerator randomStringGenerator = new Base64RandomStringGenerator();
        return new com.imaping.token.api.generator.DefaultUniqueTokenIdGenerator(
                randomStringGenerator,
                32  // Token ID 长度 (32 字符 = 256 位熵)
        );
    }

    /**
     * 自定义 Token ID 生成器 (更长的 Token ID)
     */
    @Bean
    public UniqueTokenIdGenerator longTokenIdGenerator() {
        RandomStringGenerator randomStringGenerator = new Base64RandomStringGenerator();
        return new com.imaping.token.api.generator.DefaultUniqueTokenIdGenerator(
                randomStringGenerator,
                64  // 64 字符 = 512 位熵
        );
    }
}
```

**使用自定义生成器:**

```java
package com.example.service;

import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomTokenIdService {

    private final UniqueTokenIdGenerator customTokenIdGenerator;

    /**
     * 生成自定义 Token ID
     */
    public String generateTokenId() {
        return customTokenIdGenerator.getNewTokenId("CUSTOM");
    }
}
```

---

## 4. 自定义 Token 类型

### 4.1 实现步骤

**步骤 1: 定义 Token 接口**

继承 `Token` 接口,添加自定义方法。

**步骤 2: 实现 Token 类**

继承 `AbstractToken`,实现自定义接口。

**步骤 3: 实现 TokenFactory**

实现 `TokenFactory` 接口,负责创建自定义 Token。

**步骤 4: 注册到 Spring 容器**

使用 `@Bean` 注册工厂。

---

### 4.2 完整示例: RefreshToken

**场景**: 实现一个 RefreshToken,用于刷新 AccessToken。

**步骤 1: 定义 RefreshToken 接口**

```java
package com.example.token.model;

import com.imaping.token.api.model.Token;

/**
 * 刷新令牌接口
 */
public interface RefreshToken extends Token {

    /**
     * 获取关联的 AccessToken ID
     *
     * @return AccessToken ID
     */
    String getAccessTokenId();

    /**
     * 设置关联的 AccessToken ID
     *
     * @param accessTokenId AccessToken ID
     */
    void setAccessTokenId(String accessTokenId);

    /**
     * 检查 RefreshToken 是否可以使用
     *
     * @return true 如果未过期且未被使用
     */
    boolean canRefresh();
}
```

**步骤 2: 实现 RefreshToken 类**

```java
package com.example.token.model;

import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.model.AbstractToken;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 刷新令牌实现类
 */
@NoArgsConstructor
public class DefaultRefreshToken extends AbstractToken implements RefreshToken {

    private static final long serialVersionUID = 1L;

    /**
     * RefreshToken 前缀
     */
    public static final String PREFIX = "RT";

    /**
     * 关联的 AccessToken ID
     */
    @Getter
    @Setter
    private String accessTokenId;

    /**
     * 是否已使用 (RefreshToken 只能使用一次)
     */
    private boolean used = false;

    /**
     * 构造函数
     *
     * @param id Token ID
     * @param expirationPolicy 过期策略
     * @param authentication 认证信息
     */
    public DefaultRefreshToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication) {
        super(id, expirationPolicy, authentication);
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public boolean canRefresh() {
        return !isExpired() && !used;
    }

    /**
     * 标记为已使用
     */
    public void markAsUsed() {
        this.used = true;
    }

    /**
     * 检查是否已使用
     */
    public boolean isUsed() {
        return used;
    }
}
```

**步骤 3: 实现 RefreshTokenFactory**

```java
package com.example.token.factory;

import com.example.token.model.DefaultRefreshToken;
import com.example.token.model.RefreshToken;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.HardTimeoutExpirationPolicy;
import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import com.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

/**
 * RefreshToken 工厂
 */
@RequiredArgsConstructor
public class RefreshTokenFactory implements TokenFactory {

    private final UniqueTokenIdGenerator tokenIdGenerator;
    private final long timeToLiveInSeconds;  // RefreshToken 有效期 (如 30 天)

    @Override
    public Class<? extends Token> getTokenType() {
        return RefreshToken.class;
    }

    /**
     * 创建 RefreshToken
     *
     * @param authentication 认证信息
     * @return RefreshToken
     */
    public Token createToken(Authentication authentication) {
        String tokenId = tokenIdGenerator.getNewTokenId(DefaultRefreshToken.PREFIX);

        // 创建固定时间过期策略 (RefreshToken 不自动续期)
        ExpirationPolicy expirationPolicy = new HardTimeoutExpirationPolicy(
                timeToLiveInSeconds,
                Clock.systemUTC()
        );

        return new DefaultRefreshToken(tokenId, expirationPolicy, authentication);
    }

    /**
     * 创建 RefreshToken 并关联 AccessToken
     *
     * @param authentication 认证信息
     * @param accessTokenId 关联的 AccessToken ID
     * @return RefreshToken
     */
    public RefreshToken createToken(Authentication authentication, String accessTokenId) {
        DefaultRefreshToken refreshToken = (DefaultRefreshToken) createToken(authentication);
        refreshToken.setAccessTokenId(accessTokenId);
        return refreshToken;
    }
}
```

**步骤 4: 注册到 Spring 容器**

```java
package com.example.config;

import com.example.token.factory.RefreshTokenFactory;
import com.example.token.model.RefreshToken;
import com.imaping.token.api.factory.DefaultTokenFactory;
import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshTokenConfig {

    /**
     * 注册 RefreshTokenFactory
     */
    @Bean
    public RefreshTokenFactory refreshTokenFactory(
            UniqueTokenIdGenerator tokenIdGenerator,
            @Value("${imaping.token.refreshToken.timeToLiveInSeconds:2592000}") long timeToLiveInSeconds) {
        // 默认 30 天 (2592000 秒)
        return new RefreshTokenFactory(tokenIdGenerator, timeToLiveInSeconds);
    }

    /**
     * 将 RefreshTokenFactory 注册到 DefaultTokenFactory
     */
    @Bean
    public DefaultTokenFactory defaultTokenFactory(
            DefaultTokenFactory defaultTokenFactory,
            RefreshTokenFactory refreshTokenFactory) {

        defaultTokenFactory.addTokenFactory(RefreshToken.class, refreshTokenFactory);
        return defaultTokenFactory;
    }
}
```

**使用 RefreshToken:**

```java
package com.example.service;

import com.example.token.factory.RefreshTokenFactory;
import com.example.token.model.RefreshToken;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.model.TimeoutAccessToken;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenFactory refreshTokenFactory;
    private final TokenRegistry tokenRegistry;

    /**
     * 创建 AccessToken 和 RefreshToken 对
     *
     * @param authentication 认证信息
     * @return TokenPair
     */
    public TokenPair createTokenPair(Authentication authentication, TimeoutAccessToken accessToken) {
        try {
            // 创建 RefreshToken 并关联 AccessToken
            RefreshToken refreshToken = refreshTokenFactory.createToken(
                    authentication,
                    accessToken.getId()
            );

            // 添加到注册表
            tokenRegistry.addToken(refreshToken);

            log.info("Token pair created - AccessToken: {}, RefreshToken: {}",
                    accessToken.getId(), refreshToken.getId());

            return new TokenPair(accessToken, refreshToken);

        } catch (Exception e) {
            log.error("Failed to create token pair", e);
            throw new RuntimeException("Token pair creation failed", e);
        }
    }

    /**
     * 使用 RefreshToken 刷新 AccessToken
     *
     * @param refreshTokenId RefreshToken ID
     * @return 新的 AccessToken
     */
    public TimeoutAccessToken refreshAccessToken(String refreshTokenId) {
        try {
            // 获取 RefreshToken
            RefreshToken refreshToken = tokenRegistry.getToken(refreshTokenId, RefreshToken.class);
            if (refreshToken == null) {
                throw new RuntimeException("RefreshToken not found");
            }

            // 检查是否可以刷新
            if (!refreshToken.canRefresh()) {
                throw new RuntimeException("RefreshToken expired or already used");
            }

            // 创建新的 AccessToken (使用相同的 Authentication)
            // ... (此处省略 AccessToken 创建逻辑)

            // 标记 RefreshToken 为已使用
            ((com.example.token.model.DefaultRefreshToken) refreshToken).markAsUsed();
            tokenRegistry.updateToken(refreshToken);

            log.info("AccessToken refreshed using RefreshToken: {}", refreshTokenId);

            return null; // 返回新创建的 AccessToken

        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Token 对
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TokenPair {
        private TimeoutAccessToken accessToken;
        private RefreshToken refreshToken;
    }
}
```

---

## 5. 自定义过期策略

### 5.1 实现步骤

**步骤 1: 实现 ExpirationPolicy 接口**

继承 `AbstractTokenExpirationPolicy` 或直接实现 `ExpirationPolicy`。

**步骤 2: 实现 isExpired() 方法**

定义 Token 何时过期的逻辑。

**步骤 3: 实现 getTimeToLive() 和 getTimeToIdle() 方法**

返回 Token 的生存时间和空闲时间。

**步骤 4: 在 TokenFactory 中使用**

创建 Token 时使用自定义过期策略。

---

### 5.2 完整示例: CustomExpirationPolicy

**场景**: 实现一个"工作时间过期策略",只在工作时间内有效 (周一到周五 9:00-18:00)。

**步骤 1: 实现 ExpirationPolicy**

```java
package com.example.expiration;

import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.model.Token;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * 工作时间过期策略
 *
 * <p>Token 只在工作时间内有效 (周一到周五 9:00-18:00)</p>
 */
@RequiredArgsConstructor
public class WorkingHoursExpirationPolicy implements ExpirationPolicy, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工作开始时间
     */
    private static final LocalTime WORK_START = LocalTime.of(9, 0);

    /**
     * 工作结束时间
     */
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    /**
     * 时钟
     */
    @Getter
    private final Clock clock;

    /**
     * 最大生存时间 (秒)
     */
    @Getter
    private final Long timeToLive;

    @Override
    public boolean isExpired(Token tokenState) {
        ZonedDateTime now = ZonedDateTime.now(clock);

        // 1. 检查是否超过最大生存时间
        if (timeToLive != null && timeToLive > 0) {
            ZonedDateTime expirationTime = tokenState.getCreationTime().plusSeconds(timeToLive);
            if (now.isAfter(expirationTime)) {
                return true;
            }
        }

        // 2. 检查是否在工作时间内
        return !isWorkingHours(now);
    }

    /**
     * 检查是否在工作时间内
     *
     * @param dateTime 时间
     * @return true 如果在工作时间内
     */
    private boolean isWorkingHours(ZonedDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();

        // 周一到周五
        boolean isWeekday = dayOfWeek.getValue() >= DayOfWeek.MONDAY.getValue()
                && dayOfWeek.getValue() <= DayOfWeek.FRIDAY.getValue();

        // 9:00 到 18:00
        boolean isDuringWorkHours = !time.isBefore(WORK_START) && time.isBefore(WORK_END);

        return isWeekday && isDuringWorkHours;
    }

    @Override
    public Long getTimeToIdle() {
        return 0L;  // 不支持空闲时间
    }

    @Override
    public String getName() {
        return "WorkingHoursExpirationPolicy";
    }
}
```

**步骤 2: 创建 ExpirationPolicyBuilder**

```java
package com.example.expiration;

import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.expiration.ExpirationPolicyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 工作时间过期策略构建器
 */
@Component
@RequiredArgsConstructor
public class WorkingHoursExpirationPolicyBuilder implements ExpirationPolicyBuilder {

    private final Clock clock;

    /**
     * 构建过期策略
     *
     * @param timeToLiveInSeconds 最大生存时间 (秒)
     * @return ExpirationPolicy
     */
    @Override
    public ExpirationPolicy buildExpirationPolicy(long timeToLiveInSeconds) {
        return new WorkingHoursExpirationPolicy(clock, timeToLiveInSeconds);
    }

    /**
     * 构建过期策略 (使用默认时间)
     *
     * @return ExpirationPolicy
     */
    public ExpirationPolicy buildExpirationPolicy() {
        // 默认 8 小时 (一个工作日)
        return buildExpirationPolicy(8 * 60 * 60);
    }
}
```

**步骤 3: 在 TokenFactory 中使用**

```java
package com.example.factory;

import com.example.expiration.WorkingHoursExpirationPolicyBuilder;
import com.example.token.model.WorkingHoursToken;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.expiration.ExpirationPolicy;
import com.imaping.token.api.factory.TokenFactory;
import com.imaping.token.api.generator.UniqueTokenIdGenerator;
import com.imaping.token.api.model.AbstractToken;
import com.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工作时间 Token 工厂
 */
@Component
@RequiredArgsConstructor
public class WorkingHoursTokenFactory implements TokenFactory {

    private final UniqueTokenIdGenerator tokenIdGenerator;
    private final WorkingHoursExpirationPolicyBuilder policyBuilder;

    @Override
    public Class<? extends Token> getTokenType() {
        return WorkingHoursToken.class;
    }

    /**
     * 创建工作时间 Token
     */
    public Token createToken(Authentication authentication) {
        String tokenId = tokenIdGenerator.getNewTokenId("WHT");
        ExpirationPolicy expirationPolicy = policyBuilder.buildExpirationPolicy();

        return new WorkingHoursToken(tokenId, expirationPolicy, authentication);
    }

    /**
     * 工作时间 Token 实现
     */
    private static class WorkingHoursToken extends AbstractToken {
        private static final long serialVersionUID = 1L;
        public static final String PREFIX = "WHT";

        public WorkingHoursToken(String id, ExpirationPolicy expirationPolicy, Authentication authentication) {
            super(id, expirationPolicy, authentication);
        }

        @Override
        public String getPrefix() {
            return PREFIX;
        }
    }
}
```

**步骤 4: 使用自定义过期策略**

```java
package com.example.service;

import com.example.factory.WorkingHoursTokenFactory;
import com.imaping.token.api.authentication.Authentication;
import com.imaping.token.api.model.Token;
import com.imaping.token.api.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkingHoursTokenService {

    private final WorkingHoursTokenFactory tokenFactory;
    private final TokenRegistry tokenRegistry;

    /**
     * 创建工作时间 Token
     */
    public Token createWorkingHoursToken(Authentication authentication) {
        try {
            Token token = tokenFactory.createToken(authentication);
            tokenRegistry.addToken(token);

            log.info("WorkingHoursToken created: {}", token.getId());
            return token;

        } catch (Exception e) {
            log.error("Failed to create working hours token", e);
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * 检查 Token 是否在工作时间内有效
     */
    public boolean isValidDuringWorkingHours(String tokenId) {
        Token token = tokenRegistry.getToken(tokenId);
        if (token == null) {
            return false;
        }

        boolean isValid = !token.isExpired();
        log.debug("Token {} is {} during working hours", tokenId, isValid ? "valid" : "invalid");

        return isValid;
    }
}
```

---

## 6. 最佳实践

### 6.1 异常处理

**推荐**: 始终捕获并记录异常

```java
try {
    tokenRegistry.addToken(token);
} catch (Exception e) {
    log.error("Failed to add token: {}", token.getId(), e);
    throw new RuntimeException("Token operation failed", e);
}
```

### 6.2 空值检查

**推荐**: 检查 Token 是否存在

```java
Token token = tokenRegistry.getToken(tokenId);
if (token == null) {
    log.warn("Token not found: {}", tokenId);
    return null;
}
```

### 6.3 类型安全

**推荐**: 使用泛型方法获取指定类型的 Token

```java
// 推荐
TimeoutAccessToken token = tokenRegistry.getToken(tokenId, TimeoutAccessToken.class);

// 不推荐
Token token = tokenRegistry.getToken(tokenId);
TimeoutAccessToken timeoutToken = (TimeoutAccessToken) token;  // 可能抛出 ClassCastException
```

### 6.4 使用 Optional

**推荐**: 使用 `Optional` 避免 NullPointerException

```java
public Optional<Token> findToken(String tokenId) {
    return Optional.ofNullable(tokenRegistry.getToken(tokenId));
}

// 使用
findToken(tokenId).ifPresent(token -> {
    // 处理 Token
});
```

### 6.5 日志记录

**推荐**: 记录关键操作

```java
log.info("Token created: {}, user: {}", token.getId(), userId);
log.warn("Token not found: {}", tokenId);
log.error("Failed to delete token: {}", tokenId, exception);
```

### 6.6 事务管理

**推荐**: 在需要原子性操作时使用事务

```java
@Transactional
public void transferToken(String fromUserId, String toUserId, String tokenId) throws Exception {
    Token token = tokenRegistry.getToken(tokenId);
    // 修改 Token 所有者
    tokenRegistry.updateToken(token);
}
```

### 6.7 避免过度更新

**推荐**: 只在必要时更新 Token

```java
// 不推荐: 每次读取都更新
Token token = tokenRegistry.getToken(tokenId);
token.update();
tokenRegistry.updateToken(token);

// 推荐: 只在需要时更新 (如刷新过期时间)
if (shouldRefresh) {
    token.update();
    tokenRegistry.updateToken(token);
}
```

### 6.8 清理过期 Token

**推荐**: 定期清理过期 Token (使用内存注册表时)

```java
@Scheduled(fixedDelay = 300000)  // 每 5 分钟
public void cleanupExpiredTokens() {
    long deleted = tokenRegistry.getTokens(Token::isExpired)
            .mapToInt(token -> {
                try {
                    return tokenRegistry.deleteToken(token);
                } catch (Exception e) {
                    log.error("Failed to delete expired token", e);
                    return 0;
                }
            })
            .sum();
    log.info("Cleaned up {} expired tokens", deleted);
}
```

---

**维护责任**: 架构团队
**更新频率**: API 变更时更新
**审核流程**: 技术委员会批准

**相关文档**:
- [架构文档](architecture.md)
- [配置参考](configuration.md)
- [快速开始](quick-start.md)
- [扩展点文档](architecture/8-扩展点.md)
