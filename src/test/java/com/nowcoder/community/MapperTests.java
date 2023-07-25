package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


import java.util.Date;
import java.util.List;


@SpringBootTest
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void updateUser() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);

        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
        System.out.println(rows);

        rows = userMapper.updatePassword(150, "hello");
        System.out.println(rows);
    }

    @Test
    public void testSelectPosts(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10,0);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
        int i = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(i);
    }

    @Test
    public void testInsertLoginTicket(){
//        LoginTicket loginTicket = new LoginTicket(102,0,
//                "abcd",new Date(System.currentTimeMillis() + 1000 * 60 * 10));
//        loginTicketMapper.insertLoginTicket(loginTicket);
    }
    @Test
    public void testSelectLoginTicket(){
        System.out.println(loginTicketMapper.selectByTicket("abc"));

        loginTicketMapper.updateStatus("abc",1);
        System.out.println(loginTicketMapper.selectByTicket("abc"));
    }

    @Test
    public void testSelectDiscussPost(){
        DiscussPost post = discussPostMapper.selectDiscussPsotById(154);
        System.out.println(post);
//        User user = userMapper.selectById(post.getUserId());
//        System.out.println(user);
    }

    @Test
    public void testSelectLetters(){
//        System.out.println(messageMapper.selectConversations(111, 0, 20));
//        System.out.println(messageMapper.selectConversationCount(111));
        List<Message> list = messageMapper.selectLetters("111_112", 0, 20);
        for (Message message : list) {
            System.out.println(message);
        }
        System.out.println(messageMapper.selectLetterCount("111_112"));
        System.out.println(messageMapper.selectLetterUnreadCount(131, "111_131"));
    }


}
