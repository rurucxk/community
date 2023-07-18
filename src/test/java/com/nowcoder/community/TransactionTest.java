package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TransactionTest {

    @Autowired
    private AlphaService alphaService;

//    @Test
//    public void testSave(){
//        Object o = alphaService.save1();
//        System.out.println(o);
//    }
//
//    @Test
//    public void testSave2(){
//        Object o = alphaService.save2();
//        System.out.println(o);
//    }
}
