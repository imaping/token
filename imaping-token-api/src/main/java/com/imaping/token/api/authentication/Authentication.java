package com.imaping.token.api.authentication;

import com.imaping.token.api.authentication.principal.Principal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 认证信息类 - 封装用户认证主体信息.
 *
 * <p><b>序列化要求:</b> 作为 Token 的一部分存储,并可能在分布式环境中传输,
 * 必须保留 serialVersionUID 以确保跨版本的序列化兼容性.</p>
 *
 * @author By miaoj
 * @since 0.0.1
 * @see Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Authentication<ID extends Serializable> implements Serializable {

    /**
     * 保留 serialVersionUID 以确保序列化兼容性.
     * Authentication 对象作为 Token 的一部分存储,并可能跨 JVM 传输.
     */
    @Serial
    private static final long serialVersionUID = -6367266243088834065L;

    private Principal<ID> principal;
}
