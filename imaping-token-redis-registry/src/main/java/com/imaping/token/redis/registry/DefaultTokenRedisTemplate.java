package com.imaping.token.redis.registry;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.imaping.token.api.model.Token;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefaultTokenRedisTemplate<K, V> extends RedisTemplate<K, V> implements TokenRedisTemplate<K, V> {

    public DefaultTokenRedisTemplate() {
        super();
        final RedisSerializer<String> string = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Token> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Token.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 解决jackson2无法反序列化LocalDateTime的问题
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        setKeySerializer(string);
        setValueSerializer(jackson2JsonRedisSerializer);
        setHashKeySerializer(string);
        setHashValueSerializer(string);
    }


    public DefaultTokenRedisTemplate(final RedisConnectionFactory connectionFactory) {
        this();
        setConnectionFactory(connectionFactory);
        afterPropertiesSet();
    }

    @Override
    public Stream<String> scan(String pattern, long count) {
        var scanOptions = ScanOptions.scanOptions().match(pattern);
        if (count > 0) {
            scanOptions = scanOptions.count(count);
        }
        val cursor = getConnectionFactory().getConnection().scan(scanOptions.build());
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false)
                .onClose(() -> IOUtils.closeQuietly(cursor))
                .map(key -> (String) getKeySerializer().deserialize(key))
                .distinct();
    }

    @Override
    public void initialize() {
        afterPropertiesSet();
    }
}
