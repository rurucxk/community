package com.nowcoder.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * spring 线程池配置
 */
@Configuration
/*启动定时线程池*/
@EnableScheduling
/*加@Async,让方法或者类的所有方法在多线程的环境下，被异步的调用*/
@EnableAsync
public class ThreadPoolConfig {
}
