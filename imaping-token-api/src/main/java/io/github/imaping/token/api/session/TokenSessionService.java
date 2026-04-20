package io.github.imaping.token.api.session;

import java.util.List;

/**
 * Token 会话管理服务。
 */
public interface TokenSessionService {

    /**
     * 默认 Bean 名称。
     */
    String BEAN_NAME = "tokenSessionService";

    /**
     * 统计用户当前活跃会话数量。
     */
    long countSessionsFor(String principalId);

    /**
     * 获取用户当前活跃会话列表。
     */
    List<TokenSession> getSessionsFor(String principalId);

    /**
     * 获取用户当前活跃会话列表,并标记当前会话。
     */
    List<TokenSession> getSessionsFor(String principalId, String currentTokenId);

    /**
     * 获取指定 token 的会话信息。
     */
    TokenSession getSession(String tokenId);

    /**
     * 判断 token 是否归属于指定用户且仍然活跃。
     */
    boolean isSessionActive(String principalId, String tokenId);

    /**
     * 注销指定用户的某个会话。
     *
     * @return 删除的 token 数量
     */
    int revokeSession(String principalId, String tokenId) throws Exception;

    /**
     * 注销指定用户的全部活跃会话。
     *
     * @return 删除的 token 数量
     */
    long revokeSessions(String principalId) throws Exception;

    /**
     * 注销指定用户除当前 token 外的其他活跃会话。
     *
     * @return 删除的 token 数量
     */
    long revokeOtherSessions(String principalId, String currentTokenId) throws Exception;
}
