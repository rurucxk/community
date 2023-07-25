package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    /*
        查询所有帖子
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }

    //查询帖子数量
    public int findDiscussPostRows(int userId){
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
