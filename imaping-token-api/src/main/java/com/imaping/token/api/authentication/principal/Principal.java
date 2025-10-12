package com.imaping.token.api.authentication.principal;

import com.imaping.token.core.model.BaseUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author By miaoj
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Principal implements Serializable {
    @Serial
    private static final long serialVersionUID = 4126037249642858717L;

    private String id;

    private BaseUserInfo userInfo;
}
