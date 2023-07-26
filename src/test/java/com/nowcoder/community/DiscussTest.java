package com.nowcoder.community;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

@SpringBootTest
public class DiscussTest {

    @Autowired
    public CommentService commentService;

    @Autowired
    public DiscussPostService discussPostService;

    private DiscussPost data;

    @Before
    public void before(){
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("Test Title");
        data.setContent("Test");
        data.setCreateTime(new Date());
        System.out.println(data);
        discussPostService.addDiscussPost(data);
    }

    @After
    public void after(){
        discussPostService.updateStatus(data.getId(),2);
    }

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
        DiscussPost post = discussPostService.findDiscussPostById(217);
        System.out.println(post);
    }

    @Test
    public void testFindById(){
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertNotNull(post);
        Assert.assertEquals(post.toString(),data.toString());
    }
}
