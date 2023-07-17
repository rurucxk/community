package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SensitiveTest {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter(){
        String text = "这里可以赌博，可以吸毒，可以嫖娼和开票。快来啊！赌赌博博";
        System.out.println(sensitiveFilter.filter(text));
        text = "这里可以赌🤣博，可以🤣吸🤣毒🤣，可以🤣嫖娼🤣和🤣开🤣票。🤣快来啊！";
        System.out.println(sensitiveFilter.filter(text));
        text = "☆f☆a☆bc☆d☆" + "qqfabc";
        System.out.println(sensitiveFilter.filter(text));

    }
}
