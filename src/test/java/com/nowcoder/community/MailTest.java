package com.nowcoder.community;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootTest
public class MailTest {
    @Autowired
    private MailClient mailClient;

    @Resource
    private TemplateEngine templateEngine;

    @Autowired
    private UserService userService;

    @Test
    public void testmail(){
        mailClient.sendMail("2568617273@qq.com","TEST","Welcome");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "朱凌峰");
        String content = templateEngine.process("/mail/demo", context);
        System.out.println(context);
        mailClient.sendMail("2568617273@qq.com","Html",content);
    }

    @Test
    public void testResetPassword(){
        Map<String, Object> map = userService.resetPassword("2568617273@qq.com", "1234");
        System.out.println(map.containsKey("user"));
        System.out.println(map.get("passwordMsg"));
        User user = (User) map.get("user");
//        System.out.println(user.getPassword());
//        System.out.println(user.getSalt());
//        System.out.println(CommunityUtil.md5("1234" + user.getSalt()));
//        String s1 = CommunityUtil.md5("1234" + user.getSalt());
//        System.out.println(s1.equals(s));

    }
}
