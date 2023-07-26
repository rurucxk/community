package com.nowcoder.community.Test;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class DiscussPostTests {

    @Autowired
    private DiscussPostService discussPostService;

    public DiscussPost data;

    @BeforeEach
    public void before(){
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("Test Title");
        data.setContent("Test");
        data.setCreateTime(new Date());
        System.out.println(data);
        discussPostService.addDiscussPost(data);
    }

    @AfterEach
    public void after(){
        discussPostService.updateStatus(data.getId(),2);
    }

    @Test
    public void testFindById(){
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertNotNull(post);
        Assert.assertEquals(post.toString(),data.toString());
    }

    @Test
    public void testUpdateScore(){
        int rows = discussPostService.updateScore(data.getId(), 2000.00);
        Assert.assertEquals(1,rows);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        /*2时判断小数部分前2位是否相等*/
        Assert.assertEquals(2000.00,post.getScore(),2);
    }

    @Test
    public void testFindRows(){
        int rows = discussPostService.findDiscussPostRows(111);
    }

    @Test
    public void testFindPosts(){
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, 0, 10, 1);
    }

    @Test
    public void testUpdateCommentCount(){
        int i = discussPostService.updateCommentCount(data.getId(), 10);
        Assert.assertEquals(1,i);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertEquals(post.getCommentCount(),10);
    }
    @Test
    public void testupdateType(){
        int rows = discussPostService.updateType(data.getId(), 1);
        Assert.assertEquals(1,rows);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        /*2时判断小数部分前2位是否相等*/
        Assert.assertEquals(1,post.getType());
    }
    @Test
    public void testUpdateStatus(){
        int rows = discussPostService.updateStatus(data.getId(), 1);
        Assert.assertEquals(1,rows);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        /*2时判断小数部分前2位是否相等*/
        Assert.assertEquals(1,post.getStatus());
    }

}
