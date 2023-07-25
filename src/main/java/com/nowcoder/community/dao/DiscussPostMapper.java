package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帖子
 */
@Mapper
public interface DiscussPostMapper {

    /*支持按置顶和创建时间排序，和按分数排序来实现按热度排序
    * orderMode传入0按最新排序，传入1按最热排序*/
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    //@Param取别名
    //如果只有一个参数并且在if中使用，必须加别名
    int selectDiscussPostRows(@Param("userId") int userId);


    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPsotById(int id);

    //增加评论数量
    int updateCommentCount(int id, int commentCount);

    /*修改帖子类型,0-普通; 1-置顶;*/
    int updateType(int id, int type);

    /*修改帖子状态0-正常; 1-精华; 2-删除;*/
    int updateStatus(int id, int status);

    /*更改帖子分数*/
    int updateScore(int id, double score);
}
