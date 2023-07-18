package com.nowcoder.community.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Comment {

    private int id;
    private int userId;
    // 1：帖子的评论 2：评论的回复
    private int entityType;
    // entityType为1时，帖子id；为2时，评论id
    private int entityId;
    // entityType为2且是对其他用户的回复的回复时不为0，为目标用户id
    private int targetId;
    private String content;
    private int status;
    private Date createTime;
}
