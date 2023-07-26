package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    /*最大缓存数量*/
    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    /*最大缓存时间*/
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    /*Caffeine的核心接口：Cache，LoadingCache(同步)，AsyncLoadCache(异步)*/

    /*帖子列表的缓存*/
    private LoadingCache<String, List<DiscussPost>> postListCache;

    /*帖子总数的缓存*/
    private LoadingCache<String, Integer> postRowsCache;

    @PostConstruct
    public void init(){
        /*初始化帖子列表的缓存   Caffeine -> Redis -> mysql*/
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                /*如何查询到数据*/
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(String key) throws Exception {
                        if(key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params = key.split(":");
                        if(params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.parseInt(params[0]);
                        int limit = Integer.parseInt(params[1]);

                        /*二级缓存： Redis -> mysql*/
                        List<DiscussPost> obj = (List<DiscussPost>) redisTemplate.opsForValue().get(key);
                        if(obj == null){
                            logger.debug("load post list from DB");
                            List<DiscussPost> posts = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                            redisTemplate.opsForValue().set(key,posts,maxSize * 2L,TimeUnit.SECONDS);
                            return posts;
                        }else {
                            logger.debug("load post list from Redis");
                            return obj;
                        }
                        /*不启用二级缓存*/
//                        logger.debug("load post list from DB");
//                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });


        /*初始化帖子总数的缓存 Caffeine -> Redis -> mysql*/
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public @Nullable Integer load(String key) throws Exception {

                        Integer rows = (Integer) redisTemplate.opsForValue().get(key);
                        if(rows == null || rows == 0){
                            logger.debug("load post rows from DB");
                            Integer i = Integer.valueOf(key);
                            int postRows = discussPostMapper.selectDiscussPostRows(i);
                            redisTemplate.opsForValue().set(key,postRows,maxSize * 2L,TimeUnit.SECONDS);
                            return postRows;
                        }else {
                            logger.debug("load post rows from Redis");
                            return rows;
                        }
                        /*不启用二级缓存*/
//                        logger.debug("load post list from DB");
//                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    /*查询所有帖子*/
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        /*只缓存热门的帖子就是orderMode,不管用户是否登录，当userId为0时，就是查询所有帖子*/
        if(userId == 0 && orderMode == 1){
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }

    //查询帖子数量
    public int findDiscussPostRows(int userId){
        if(userId == 0){
            return postRowsCache.get(String.valueOf(userId));
        }

        logger.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //发布帖子
    public int addDiscussPost(DiscussPost post){
        if(post == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义HTML标记，springMvc的工具将<,>转义成字符
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    //查询帖子
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPsotById(id);
    }

    //更新评论数量
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    /*置顶和取消置顶0-普通; 1-置顶;*/
    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }

    /*加精和删除0-正常; 1-精华; 2-删除;*/
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id,status);
    }

    /*更新帖子分数*/
    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id,score);
    }

}
