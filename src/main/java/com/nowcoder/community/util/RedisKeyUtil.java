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
        关注的目标
     */
    private static final String PREFIX_FOLLOWEE = "followee";
    /*
        粉丝
     */
    private static final String PREFIX_FOLLOWER = "follower";

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

    /*
        某个用户关注的实体（人，帖子...）
        followee:userId:entityType ->zset(entityId,now)
     */
    public static String getFolloweeKey(int userId, int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /*
        某个实体拥有的粉丝
        follower:entityType:entityId ->zset(userId,now)
     */
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }
}
