package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootTest
public class WkTests {

    @Value("${wk.image.storage}")
    private String wkImageStorage;


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

    @Test
    public void deleteFiles() throws IOException {
        File directory = new File(wkImageStorage);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if(files != null) {
                for (File file : files) {
                    System.out.println(file.toPath());
                    System.out.println(Files.getLastModifiedTime(file.toPath()).toMillis());
                    System.out.println(System.currentTimeMillis());
                    System.out.println(file.getName());
                }
            }
        }
    }
}
