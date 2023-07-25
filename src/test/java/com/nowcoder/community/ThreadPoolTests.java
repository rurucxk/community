package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.*;

@SpringBootTest
public class ThreadPoolTests {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    @Autowired
    private AlphaService alphaService;

    /*JDK的普通线程池*/
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    /*JDK可执行定时任务的线程池*/
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    /*spring 普通线程池*/
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /*spring 定时线程池*/
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*JDK普通线程池*/
    @Test
    public void testExecutorService(){
        Runnable test = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ExecutorService");
            }
        };
        for (int i = 0; i < 10; i++) {
            executorService.submit(test);
        }
        sleep(10000);
    }
    /*JDK定时任务线程池*/
    @Test
    public void testScheduledExecutorService(){
        Runnable test = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ScheduledExecutorService");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(test,10000,1000, TimeUnit.MILLISECONDS);
        sleep(30000);
    }

    /*spring 普通线程池*/
    @Test
    public void testTaskExecutor(){
        Runnable test = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ThreadPoolTaskExecutor");
            }
        };
        for (int i = 0; i < 10; i++) {
            taskExecutor.submit(test);
        }
        sleep(10000);
    }

    /*spring 定时线程池*/
    @Test
    public void testTaskScheduler(){
        Runnable test = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ThreadPoolTaskScheduler");
            }
        };
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        taskScheduler.scheduleAtFixedRate(test,startTime,1000);
        sleep(30000);
    }

    /*spring 普通线程池的简化方式，通过注解*/
    @Test
    public void testThreadPoolTaskExecutorSimple(){
        for (int i = 0; i < 10; i++) {
            alphaService.execute1();
        }
        sleep(10000);
    }

    /*spring 定时线程池的简化方式，通过注解*/
    @Test
    public void testThreadPoolTaskSchedulerSimple(){
        sleep(30000);
    }
}
