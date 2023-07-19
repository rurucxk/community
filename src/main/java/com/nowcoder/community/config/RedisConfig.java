package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis配置
 */
@Configuration
public class RedisConfig {

    /**
        RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        /**
            序列化方式
         */

        /*
            设置key的序列化方式
         */
        template.setKeySerializer(RedisSerializer.string());
        /*
            设置value的序列化方式
         */
        template.setValueSerializer(RedisSerializer.json());
        /*
            设置hash key的序列化方式
         */
        template.setHashKeySerializer(RedisSerializer.string());
        /*
            设置hash value的序列化方式
         */
        template.setHashValueSerializer(RedisSerializer.json());

        /*
            使配置生效
         */
        template.afterPropertiesSet();

        return template;

    }
}
