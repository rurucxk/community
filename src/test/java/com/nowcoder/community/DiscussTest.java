package com.nowcoder.community;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DiscussTest {

    @Autowired
    public CommentService commentService;

    @Autowired
    public DiscussPostService discussPostService;

    @Test
    public void testHuifu(){
        List<Comment> list = commentService.findCommentByEntity(1, 234, 0, 5);
        System.out.println(list);
        List<Comment> replyList = commentService.findCommentByEntity(2,
                43, 0, Integer.MAX_VALUE);
        System.out.println(replyList);
    }

    @Test
    public void testDiscussPost(){
        DiscussPost post = discussPostService.findDiscussPost(217);
        System.out.println(post);
    }
}
