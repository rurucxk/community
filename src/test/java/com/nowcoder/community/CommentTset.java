package com.nowcoder.community;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CommentTset {

    @Autowired
    private CommentService commentService;

    @Test
    public void testCountComment(){
//        System.out.println(commentService.findCountCommentByUserId(111));
//        System.out.println(commentService.findCountByEntity(1, 228));
        List<Comment> reply = commentService.findUserPostReply(154, 0, 5);
        for (Comment comment : reply) {
            System.out.println(comment);
        }
        System.out.println(commentService.findUserPostReplyCount(154));
    }

}
