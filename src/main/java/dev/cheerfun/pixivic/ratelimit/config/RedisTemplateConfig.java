package dev.cheerfun.pixivic.ratelimit.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-08 14:39
 * @description redisTemplate配置
 */
@Configuration
public class RedisTemplateConfig {
    @Bean("limitRedisTemplate")
    public RedisTemplate<String, Integer> limitRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Integer> limitRedisTemplate = new RedisTemplate<>();
        //只能对字符串的键值操作
        limitRedisTemplate.setConnectionFactory(factory);
        //使用Jackson2JsonRedisSerialize 替换默认序列化(默认采用的是JDK序列化)
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        limitRedisTemplate.setKeySerializer(jackson2JsonRedisSerializer);
        limitRedisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        limitRedisTemplate.setHashKeySerializer(jackson2JsonRedisSerializer);
        limitRedisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        limitRedisTemplate.afterPropertiesSet();
        return limitRedisTemplate;
    }
}
