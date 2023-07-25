package com.nowcoder.community.config;

import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * Quartz:分布式定时线程池
 * 默认从内存中读取配置，可以配置持久化到数据库中
 * 第一次先进行配置并存入内存，再存入数据库，然后以后quartz访问数据库来获取信息
 */
@Configuration
public class QuartzConfing {

    /*FactoryBean:简化Bean的实例化过程
    * 1.Spring通过FactoryBean封装了Bean的实例化过程
    * 2.可以将FactoryBean装配到Spring容器中
    * 3.将FactoryBean注入给其他的Bean
    * 4.该Bean得到的是FactoryBean所管理的对象实例
    * SimpleTriggerFactoryBean的参数JobDetail，传入的bean是JobDetailFactoryBean封装的JobDetail
    * 就是将JobDetail加上@Bean等于JobDetailFactoryBean加上@Bean，但是JobDetailFactoryBean会更简单*/

    /**
     * 配置JobDetail
     */
//    @Bean
    public JobDetailFactoryBean alphaJobDetail(){

        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        /*配置的是哪个Job*/
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaJobGroup");
        /*任务是否长期保存*/
        factoryBean.setDurability(true);
        /*任务是否可以被恢复*/
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    /**
     *配置Trigger
     * 可以使用SimpleTriggerFactoryBean：简单的Trigger（每10分钟触发）
     *        CronTriggerFactoryBean:复杂逻辑的Trigger（每个月的月底8点触发，每周周五8点触发）
     *
     */
//    @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){

        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        /*是哪个JobDetail的触发器*/
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        /*多久执行一次 ms*/
        factoryBean.setRepeatInterval(3000);
        /*Job状态*/
        factoryBean.setJobDataMap(new JobDataMap());

        return factoryBean;
    }

    /**
     *刷新帖子分数任务
     */
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){

        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        /*配置的是哪个Job*/
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        /*任务是否长期保存*/
        factoryBean.setDurability(true);
        /*任务是否可以被恢复*/
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){

        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        /*是哪个JobDetail的触发器*/
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        /*多久执行一次 5分钟*/
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        /*Job状态*/
        factoryBean.setJobDataMap(new JobDataMap());

        return factoryBean;
    }
}
