package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    //查询单个帖子的回复
    List<Comment> selectCommentByEntity(int entityType, int entityId, int offset, int limit);

    //查询数目
    int selectCountByEntity(int entityType, int entityId);

    //增加评论
    int insertComment(Comment comment);

    /*
        查询用户的回复数量
     */
    int selectCountCommentByUserId(int userId);

    /*
        查询用户回复的帖子
     */
    List<Comment> selectUserPostReply(int userId, int offset, int limit);

    /*
        查询用户回复的帖子的数量
     */
    int selectUserPostReplyCount(int userId);

}
