package com.nowcoder.community.util;

/**
 * Redis的key管理
 */
public class RedisKeyUtil {

    /*
        key的拼接符，redis的拼接为xx:xx
     */
    private static final String SPLIT = ":";

    /*
        帖子的赞前缀
     */
    private static final String PREFIX_ENTITY_LIKE = "like:entity";

    /*
        用户的赞前缀
     */
    private static final String PREFIX_USER_LIKE = "like:user";
    /*
        某个实体的赞
        like:entity:entityType:entityId -> set(userId)
     */
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /*
        某个用户收到的赞
        like:user:userId -> id
     */
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }
}
