package com.imaping.token.api.registry;

import com.imaping.token.api.model.Token;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractMapBasedTokenRegistry extends AbstractTokenRegistry {

    @Override
    public void addTokentInternal(final Token token) throws Exception {
        log.debug("Putting token [{}] in registry.", token.getId());
        getMapInstance().put(token.getId(), token);
    }

    @Override
    public Token getToken(final String tokenId, final Predicate<Token> predicate) {
        if (StringUtils.isBlank(tokenId)) {
            return null;
        }
        val result = getMapInstance().get(tokenId);
        if (result == null) {
            log.debug("Token [{}] could not be found", tokenId);
            return null;
        }
        if (!predicate.test(result)) {
            log.debug("Cannot successfully fetch token [{}]", tokenId);
            return null;
        }
        return result;
    }

    @Override
    public long deleteSingleToken(final String tokenId) {
        return !StringUtils.isBlank(tokenId) && getMapInstance().remove(tokenId) != null ? 1 : 0;
    }

    @Override
    public long deleteAll() {
        val size = getMapInstance().size();
        getMapInstance().clear();
        return size;
    }

    @Override
    public Collection<? extends Token> getTokens() {
        return getMapInstance().values();
    }

    @Override
    public Token updateToken(final Token token) throws Exception {
        log.trace("Updating token [{}] in registry...", token.getId());
        addToken(token);
        return token;
    }

    /**
     * Create map instance, which must ben created during initialization phases
     * and always be the same instance.
     *
     * @return the map
     */
    public abstract Map<String, Token> getMapInstance();
}
