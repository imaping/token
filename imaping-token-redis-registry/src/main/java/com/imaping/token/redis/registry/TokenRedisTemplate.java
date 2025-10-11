package com.imaping.token.redis.registry;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;

import java.util.stream.Stream;

public interface TokenRedisTemplate<K, V> extends RedisOperations<K, V> {

    /**
     * Keys stream.
     *
     * @param pattern the pattern
     * @param count   the count
     * @return the stream
     */
    Stream<String> scan(String pattern, long count);

    /**
     * Initialize.
     */
    void initialize();

    /**
     * Gets connection factory.
     *
     * @return the connection factory
     */
    RedisConnectionFactory getConnectionFactory();
}
