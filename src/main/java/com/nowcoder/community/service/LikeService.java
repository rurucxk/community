package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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
        开启编程式事务
     */
    public void like(int userId, int entityType, int entityId, int entityUserId){
//        /*
//            获取key
//         */
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
//
//        /*
//            判断是否已经点赞
//         */
//        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//
//        if(isMember){
//            redisTemplate.opsForSet().remove(entityLikeKey,userId);
//        }else {
//            redisTemplate.opsForSet().add(entityLikeKey,userId);
//        }

        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                /*
                    获取key
                */
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                /*
                    判断是否已经点赞,查询要放在事务开启之前
                */
                boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                /*
                    开启事务
                 */
                operations.multi();

                if(isMember){
                    operations.opsForSet().remove(entityLikeKey,userId);
                    operations.opsForValue().decrement(userLikeKey);
                }else {
                    operations.opsForSet().add(entityLikeKey,userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec();
            }
        });
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

    /**
     * 查询某个用户收到的赞数量
     */
    public int findUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count;
    }
}
