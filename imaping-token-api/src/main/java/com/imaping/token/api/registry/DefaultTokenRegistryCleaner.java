package com.imaping.token.api.registry;

import com.imaping.token.api.lock.LockRepository;
import com.imaping.token.api.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.lambda.Unchecked;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
//@Transactional(transactionManager = "tokenTransactionManager")
public class DefaultTokenRegistryCleaner implements TokenRegistryCleaner {
    private final LockRepository lockRepository;

    private final TokenRegistry tokenRegistry;

    @Override
    public int clean() {
        try {
            if (!isCleanerSupported()) {
                log.trace("Token registry cleaner is not supported by [{}]", getClass().getSimpleName());
                return 0;
            }
            return cleanInternal();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int cleanToken(final Token token) {
        return lockRepository.execute(token.getId(), Unchecked.supplier(() -> {
            log.debug("Cleaning up expired token [{}]", token.getId());
            return tokenRegistry.deleteToken(token);
        })).orElseThrow();
    }

    protected int cleanInternal() {
        try (val expiredTokens = tokenRegistry.stream().filter(Objects::nonNull).filter(Token::isExpired)) {
            val tokensDeleted = expiredTokens
                    .mapToInt(this::cleanToken)
                    .sum();
            log.debug("[{}] expired tokens removed.", tokensDeleted);
            return tokensDeleted;
        }
    }

    /**
     * Indicates whether the registry supports automated token cleanup.
     * Generally, a registry that is able to return a collection of available
     * tokens should be able to support the cleanup process. Default is {@code true}.
     *
     * @return true/false.
     */
    protected boolean isCleanerSupported() {
        return true;
    }
}
