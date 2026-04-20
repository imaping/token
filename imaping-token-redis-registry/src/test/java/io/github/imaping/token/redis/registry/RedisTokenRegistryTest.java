package io.github.imaping.token.redis.registry;

import io.github.imaping.token.api.authentication.Authentication;
import io.github.imaping.token.api.authentication.principal.Principal;
import io.github.imaping.token.api.expiration.TimeoutExpirationPolicy;
import io.github.imaping.token.api.model.DefaultTimeoutAccessToken;
import io.github.imaping.token.api.model.Token;
import io.github.imaping.token.core.model.BaseUserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisTokenRegistryTest {

    @Test
    void shouldStoreTokenAndLookupSessionsByPrincipal() throws Exception {
        RedisTemplateFixture fixture = new RedisTemplateFixture();
        RedisTokenRegistry registry = new RedisTokenRegistry(fixture.template);
        DefaultTimeoutAccessToken token = token("AT-1", "user-1", 120);

        registry.addToken(token);

        assertNotNull(registry.getToken(token.getId()));
        assertEquals(1, registry.getSessionsFor("user-1").count());
        assertTrue(fixture.ttlByKey.containsKey("imaping.token:data:" + token.getId()));
        assertTrue(fixture.ttlByKey.get("imaping.token:data:" + token.getId()) > 0);
    }

    @Test
    void shouldCleanupPrincipalIndexWhenTokenDeleted() throws Exception {
        RedisTemplateFixture fixture = new RedisTemplateFixture();
        RedisTokenRegistry registry = new RedisTokenRegistry(fixture.template);
        DefaultTimeoutAccessToken token = token("AT-1", "user-1", 120);
        registry.addToken(token);

        registry.deleteToken(token.getId());

        assertNull(registry.getToken(token.getId()));
        assertEquals(0, registry.getSessionsFor("user-1").count());
    }

    private DefaultTimeoutAccessToken token(final String tokenId, final String principalId, final long idleSeconds) {
        TimeoutExpirationPolicy policy = new TimeoutExpirationPolicy(idleSeconds);
        DefaultTimeoutAccessToken token = new DefaultTimeoutAccessToken(tokenId, policy, authentication(principalId));
        token.setLastTimeUsed(ZonedDateTime.now(ZoneOffset.UTC));
        return token;
    }

    private Authentication<String> authentication(final String principalId) {
        return Authentication.<String>builder()
                .principal(Principal.<String>builder()
                        .id(principalId)
                        .userInfo(BaseUserInfo.<String>builder()
                                .id(principalId)
                                .loginName(principalId)
                                .build())
                        .build())
                .build();
    }

    private static final class RedisTemplateFixture {
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        private final Map<String, Set<Object>> sets = new ConcurrentHashMap<>();
        private final Map<String, Long> ttlByKey = new ConcurrentHashMap<>();
        private final TokenRedisTemplate<String, Object> template = mock(TokenRedisTemplate.class);

        private RedisTemplateFixture() {
            ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
            @SuppressWarnings("unchecked")
            SetOperations<String, Object> setOperations = mock(SetOperations.class);

            when(template.opsForValue()).thenReturn(valueOperations);
            when(template.opsForSet()).thenReturn(setOperations);
            when(template.hasKey(anyString())).thenAnswer(invocation -> exists(invocation.getArgument(0)));
            when(template.delete(anyString())).thenAnswer(invocation -> deleteKey(invocation.getArgument(0)));
            when(template.delete(anyCollection())).thenAnswer(invocation -> deleteKeys(invocation.getArgument(0)));
            when(template.scan(anyString(), anyLong())).thenAnswer(invocation -> scan(invocation.getArgument(0)));
            when(template.boundValueOps(anyString())).thenAnswer(invocation -> boundValueOps(invocation.getArgument(0)));

            doAnswer(invocation -> {
                setValue(invocation.getArgument(0), invocation.getArgument(1), null);
                return null;
            }).when(valueOperations).set(anyString(), any());
            doAnswer(invocation -> {
                setValue(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                return null;
            }).when(valueOperations).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));

            doAnswer(invocation -> {
                sets.computeIfAbsent(invocation.getArgument(0), key -> new LinkedHashSet<>()).add(invocation.getArgument(1));
                return 1L;
            }).when(setOperations).add(anyString(), any());
            doAnswer(invocation -> {
                Set<Object> members = sets.get(invocation.getArgument(0));
                if (members == null) {
                    return 0L;
                }
                return members.remove(invocation.getArgument(1)) ? 1L : 0L;
            }).when(setOperations).remove(anyString(), any());
            when(setOperations.members(anyString())).thenAnswer(invocation -> {
                Set<Object> members = sets.get(invocation.getArgument(0));
                return members == null ? Set.of() : new LinkedHashSet<>(members);
            });
            when(setOperations.size(anyString())).thenAnswer(invocation -> {
                Set<Object> members = sets.get(invocation.getArgument(0));
                return members == null ? 0L : (long) members.size();
            });
        }

        private BoundValueOperations<String, Object> boundValueOps(final String key) {
            @SuppressWarnings("unchecked")
            BoundValueOperations<String, Object> operations = mock(BoundValueOperations.class);
            when(operations.get()).thenAnswer(invocation -> {
                purgeIfExpired(key);
                return values.get(key);
            });
            return operations;
        }

        private void setValue(final String key, final Object value, final Long timeoutSeconds) {
            values.put(key, value);
            if (timeoutSeconds != null && timeoutSeconds > 0) {
                ttlByKey.put(key, timeoutSeconds);
            } else {
                ttlByKey.remove(key);
            }
        }

        private Boolean exists(final String key) {
            purgeIfExpired(key);
            return values.containsKey(key) || sets.containsKey(key);
        }

        private Boolean deleteKey(final String key) {
            boolean deleted = values.remove(key) != null;
            ttlByKey.remove(key);
            deleted = sets.remove(key) != null || deleted;
            return deleted;
        }

        private Long deleteKeys(final Collection<String> keys) {
            long deleted = 0;
            for (String key : keys) {
                if (Boolean.TRUE.equals(deleteKey(key))) {
                    deleted++;
                }
            }
            return deleted;
        }

        private Stream<String> scan(final String pattern) {
            String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            return Stream.concat(values.keySet().stream(), sets.keySet().stream())
                    .filter(key -> key.startsWith(prefix))
                    .distinct();
        }

        private void purgeIfExpired(final String key) {
            if (!ttlByKey.containsKey(key)) {
                return;
            }
            // 测试场景只需要记录 TTL 是否被设置,不模拟真实时间流逝。
        }
    }
}
