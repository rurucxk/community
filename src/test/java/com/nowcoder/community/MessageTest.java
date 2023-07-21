package com.nowcoder.community;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MessageTest {

    @Autowired
    private MessageService messageService;

    @Test
    public void testMessage(){
        Message comment = messageService.findLatestNotice(111, "comment");
        System.out.println(comment);
    }
}
