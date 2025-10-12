package com.imaping.token.api.authentication.principal;

import com.imaping.token.core.model.BaseUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 认证主体信息 - 封装用户身份标识和详细信息.
 *
 * <p><b>序列化要求:</b> 作为 Authentication 的一部分,随 Token 一起存储,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author By miaoj
 * @since 0.0.1
 * @see Authentication
 * @see com.imaping.token.core.model.BaseUserInfo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Principal implements Serializable {
    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * Principal 对象随 Token 一起存储在 Redis 中.
     */
    @Serial
    private static final long serialVersionUID = 4126037249642858717L;

    private String id;

    private BaseUserInfo userInfo;
}
