package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    /**
     * 查询当前用户的会话列表，针对每个会话只会展示一条最新的私信
     */
    List<Message> selectConversations(int userId, int offset, int limit);

    /**
     * 查询当前用户的会话数量
     * @param userId
     * @return
     */
    int selectConversationCount(int userId);

    /**
     * 查询某个会话所包含的私信列表
     */
    List<Message> selectLetters(String conversationId,int offset, int limit);

    /**
     * 查询某个会话包含的私信数量
     */
    int selectLetterCount(String conversationId);

    /**
     * 查询未读私信的数量(包含和一个用户的未读私信，和所有用户的未读私信)
     * 对conversationId进行动态的拼接
     */
    int selectLetterUnreadCount(int userId, String conversationId);

    /**
     * 增加私信
     */
    int insertMessage(Message message);

    /**
     * 更改信息状态
     */
    int updateStatus(List<Integer> ids, int status);

    /**
     * 查询某个主题下的最新的通知
     */
    Message selectLatestNotice(int userId, String topic);

    /**
     * 查询某个主题所包含的通知数量
     */
    int selectNoticeCount(int userId, String topic);

    /**
     * 查询某个主题未读的通知数量(topic为空，则查询所有未读的通知)
     */
    int selectNoticeUnreadCount(int userId, String topic);

    /**
     * 查询某个主题所包含的所有通知
     */
    List<Message> selectNotices(int userId, String topic, int offset, int limit);



}
