package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞
 */
@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId){

        User user = hostHolder.getUser();

        /*
            点赞
         */
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        /*
            数量
         */
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        /*
            状态
         */
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus",likeStatus);
        /**
         * kafka
         */
        /*触发点赞事件，向被点赞者发送系统通知*/
        /*点赞才触发，取消赞不触发*/
        if(likeStatus == 1){
            Event event = new Event()
                    .setTopic(TOPIC_LiKE)
                    .setUserId(user.getId())
                    .setEntityId(entityId)
                    .setEntityType(entityType)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }
        /*只有对帖子点赞需要更新分数*/
        if(entityType == ENTITY_TYPE_POST){
            /*计算帖子分数，将帖子的id存入redis，等待定时任务来更新帖子分数*/
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }

        return CommunityUtil.getJSONString(0,null,map);

    }
}
