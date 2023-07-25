package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 定时更新帖子的分数
 */
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /*论坛成立的日期*/
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化论坛成日日期失败");
        }
    }


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        /*BoundSetOperations提供了一系列操作Set的方法，如添加元素、删除元素、获取所有元素等*/
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if(operations.size() == 0){
            logger.info("[任务取消] 没有需要刷新的帖子]");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数" + operations.size());

        while (operations.size() > 0){
            this.refresh((Integer)operations.pop());
        }

        logger.info("[任务结束] 帖子分数刷新完毕");
    }

    private void refresh(int postId){
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if(post == null || post.getStatus() == 2){
            logger.error("该帖子不存在：id = " + postId);
            return;
        }

        /*是否加精*/
        boolean wonderful = post.getStatus() == 1;
        /*评论数量*/
        int commentCount = post.getCommentCount();
        /*点赞数量*/
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        /*计算权重*/
        double w =(wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;

        /*分数 = 帖子权重 + 距离论坛成立时间(天)*/
        double score = Math.log10(Math.max(1,w))
                + (post.getCreateTime().getTime() - epoch.getTime())/(1000 * 3600 * 24);

        /*更新帖子分数*/
        discussPostService.updateScore(postId, score);

        /*通过kafka异步的将帖子发送到Elasticsearch上*/
        /*将未更新的post分数更新*/
        post.setScore(score);
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(post.getUserId())
                .setEntityId(postId)
                .setEntityType(ENTITY_TYPE_POST);
        /*kafka的消息生产者生产event*/
        eventProducer.fireEvent(event);
    }
}
