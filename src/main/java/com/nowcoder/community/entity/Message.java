package com.nowcoder.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * 私信消息
 */
@Data
public class Message {

    private int id;
    /**
     * 1为系统用户，2为普通用户
     */
    private int fromId;
    private int toId;
    /**
     * 0-未读;1-已读;2-删除;
     */
    private int status;
    /**
     * 拼接fromId和toId，小的在前,如111_112
     */
    private String conversationId;
    private String content;
    private Date createTime;

}
