package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootTest
public class QuartzTests {

    @Autowired
    private Scheduler scheduler;

    /*清除定时任务配置*/
    @Test
    public void testDeleteJob(){
        try {
            boolean b = scheduler.deleteJob(new JobKey("postScoreRefreshJob", "communityJobGroup"));
            System.out.println(b);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }

    }

}
