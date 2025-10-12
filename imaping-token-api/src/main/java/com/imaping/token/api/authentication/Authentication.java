package com.imaping.token.api.authentication;

import com.imaping.token.api.authentication.principal.Principal;
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
public class Authentication implements Serializable {

    @Serial
    private static final long serialVersionUID = -6367266243088834065L;

    private Principal principal;
}
