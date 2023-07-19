package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 赞的Service
 */
@Service
public class LikeService {

    @Autowired
    public RedisTemplate redisTemplate;

    /**
        点赞
     */
    public void like(int userId, int entityType, int entityId){
        /*
            获取key
         */
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);

        /*
            判断是否已经点赞
         */
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);

        if(isMember){
            redisTemplate.opsForSet().remove(entityLikeKey,userId);
        }else {
            redisTemplate.opsForSet().add(entityLikeKey,userId);
        }
    }

    /**
        查询实体点赞数量
     */
    public long findEntityLikeCount(int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /**
     * 查询某人对某实体的点赞状态(没赞，已赞，踩(未开发))
     * 1已赞，0未赞
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey,userId) ? 1 : 0;
    }

}
