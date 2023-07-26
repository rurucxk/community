package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.checkerframework.checker.units.qual.A;
import org.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Date;

@SpringBootTest
public class SpringBootTests {

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeClass
    public static void beforeClass(){
        System.out.println("beforeClass");
    }
    @AfterClass
    public static void afterClass(){
        System.out.println("afterClass");
    }
    @BeforeEach
    public void before(){
        System.out.println("before");
        /*初始化测试数据*/
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("Test Title");
        data.setContent("Test");
        data.setCreateTime(new Date());
        System.out.println(data);
        discussPostService.addDiscussPost(data);
        System.out.println(data);
    }
    @AfterEach
    public void after(){
        System.out.println("after");

        /*删除测试数据*/
        discussPostService.updateStatus(data.getId(),2);
    }

    @Test
    public void test1(){
        System.out.println("test1");
    }

    @Test
    public void test2(){
        System.out.println("test2");
    }

//    @Test
//    public void testFindById(){
//        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
//        Assert.assertNotNull(post);
//        Assert.assertEquals(post.toString(),data.toString());
//    }
//
//    @Test
//    public void testUpdateScore(){
//        int rows = discussPostService.updateScore(data.getId(), 2000.00);
//        Assert.assertEquals(1,rows);
//        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
//        /*2时判断小数部分前2位是否相等*/
//        Assert.assertEquals(2000.00,post.getScore(),2);
//
//    }
}
