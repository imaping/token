package com.imaping.token.configuration.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 并发会话控制配置。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ConcurrentSessionProperties {

    /**
     * 是否启用同账号并发会话控制。
     */
    private boolean enabled = false;

    /**
     * 同一账号允许同时在线的最大会话数。
     */
    private int maxSessions = 1;

    /**
     * 超限后的处理策略。
     */
    private ConcurrentSessionOverflowStrategy overflowStrategy = ConcurrentSessionOverflowStrategy.INVALIDATE_OLDEST;

    /**
     * 启用并发会话控制的 Token 类型列表。
     * 支持简单类名或完整类名。
     */
    private List<String> enabledTokenTypes = new ArrayList<>(List.of("TimeoutAccessToken"));
}
