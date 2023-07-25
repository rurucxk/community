package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class WkTests {

    @Test
    public void testPdf(){
        String cmd = "d:/1/wkhtmltopdf/bin/wkhtmltopdf https://www.nowcoder.com D:/1/data/wk-pdfs/1.pdf";
        try {
            Thread.sleep(1000);
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
