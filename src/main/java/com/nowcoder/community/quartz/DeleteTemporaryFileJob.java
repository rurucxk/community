package com.nowcoder.community.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 定时删除临时文件
 */
public class DeleteTemporaryFileJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(DeleteTemporaryFileJob.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        logger.info("开始清除临时文件");

        File directory = new File(wkImageStorage);
        if (directory.exists() && directory.isDirectory()){
            File[] files = directory.listFiles();
            if(files.length != 0){
                for (File file : files) {
                    try {
                        long millis = Files.getLastModifiedTime(file.toPath()).toMillis();
                        if(System.currentTimeMillis() - millis >= 1000 * 60){
                            logger.info("正在删除文件[{}]", file.getName());
                            file.delete();
                        }
                    } catch (IOException e) {
                        logger.error("删除文件失败: [{}]", file.getName());
                    }
                }
            }else {
                logger.info("[任务取消]: 没有需要清除的文件");
                return;
            }
        }else {
            logger.info("[任务取消]: 文件目录不正确或者文件目录不存在");
            return;
        }

        logger.info("[任务完成]: 已经清除所有临时文件");

    }
}
