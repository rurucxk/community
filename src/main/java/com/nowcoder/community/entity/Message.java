package com.nowcoder.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * 私信消息
 */
@Data
public class Message {

    private int id;
    private int fromId;
    private int toId;
    private int status;
    private String conversationId;
    private String content;
    private Date createTime;

}
