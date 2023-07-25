package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostContreller implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403, "请先登录");
        }
        DiscussPost post = new DiscussPost();
        post.setTitle(title);
        post.setContent(content);
        post.setUserId(user.getId());
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        /*触发发帖事件，将帖子通过kafka异步的发送到elasticsearch服务器上*/
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityId(post.getId())
                .setEntityType(ENTITY_TYPE_POST);
        /*kafka的消息生产者生产event*/
        eventProducer.fireEvent(event);
        
        /*计算帖子分数，将帖子的id存入redis，等待定时任务来更新帖子分数*/
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,post.getId());


        //报错的情况会统一处理
        return CommunityUtil.getJSONString(0,"发送成功");
    }

    //page:接受整理分页内容
    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int id, Model model, Page page){
        //查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(id);
        model.addAttribute("post", post);
        //帖子作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, id);
        model.addAttribute("likeCount",likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser() ==null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, id);
        model.addAttribute("likeStatus",likeStatus);

        /*直接通过post判断，不需要传给前端*/
//        /*置顶状态*/
//        model.addAttribute("topType", post.getType());
//
//        /*加精状态*/
//        model.addAttribute("wonderfulStatus",post.getStatus());


        //评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + id);
        page.setRows(post.getCommentCount());

        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.findCommentByEntity(ENTITY_TYPE_POST, post.getId(),
                page.getOffset(), page.getLimit());
        // 评论VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(!commentList.isEmpty()){
            for (Comment comment : commentList) {
                //评论Vo
                Map<String, Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment", comment);
                //作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser() ==null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentByEntity(ENTITY_TYPE_COMMENT,
                        comment.getId(), 0, Integer.MAX_VALUE);
                //回复的Vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if(!replyList.isEmpty()){
                    for(Comment reply : replyList) {
                        // 回复VO
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 用户
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);

                        //点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser() ==null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus",likeStatus);



                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                //回复数量
                int replyCount = commentService.findCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                commentVoList.add(commentVo);

            }
        }

        model.addAttribute("comments", commentVoList);
//        return "/index";
        return "/site/discuss-detail";
    }

    /*置顶 1-置顶， 0-普通*/
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id, int type){
        discussPostService.updateType(id,type);
        /*触发帖子更新事件，通过kafka将最新的帖子发送到Elasticsearch中*/
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityId(id)
                .setEntityType(ENTITY_TYPE_POST);

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }


    /*加精 1-加精 0-普通*/
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id,int status){
        discussPostService.updateStatus(id,status);
        /*触发帖子更新事件，通过kafka将最新的帖子发送到Elasticsearch中*/
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityId(id)
                .setEntityType(ENTITY_TYPE_POST);
        eventProducer.fireEvent(event);

        /*计算帖子分数，将帖子的id存入redis，等待定时任务来更新帖子分数*/
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJSONString(0);
    }

    /*删除*/
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);

        /*触发帖子删除事件，在Elasticsearch中删除帖子*/
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityId(id)
                .setEntityType(ENTITY_TYPE_POST);

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}

