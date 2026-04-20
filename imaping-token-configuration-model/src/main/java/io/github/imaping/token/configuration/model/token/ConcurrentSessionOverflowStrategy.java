package io.github.imaping.token.configuration.model.token;

/**
 * 并发会话超限后的处理策略。
 */
public enum ConcurrentSessionOverflowStrategy {
    /**
     * 删除最早创建的旧会话，为新登录让位。
     */
    INVALIDATE_OLDEST,

    /**
     * 拒绝新的登录请求，保留现有会话。
     */
    DENY_NEW_LOGIN
}

