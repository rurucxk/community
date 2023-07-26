package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * 消息的消费者(kafka)
 * 被动触发
 */
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;


    /*处理评论，点赞，关注的事件*/
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LiKE})
    public void handleCommentMessage(ConsumerRecord record){
        /*recode接收相关数据*/
        if(record == null || record.value() == null){
            logger.error("消息的内容为空");
            return;
        }
        /*将接收的消息反序列化*/
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if(event == null){
            logger.error("消息格式错误");
            return;
        }
        /*发送站内通知*/
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        /*系统通知的消息内容*/
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId", event.getEntityId());

        /*额外的数据*/
        if(!event.getData().isEmpty()){
            /*迭代entrySet获取key，value的集合*/
/*            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(),entry.getValue());
            }*/

            /*迭代可替换为大量的 'Map.putAll' 调用 */
            content.putAll(event.getData());
        }

        /*将消息内容序列化*/
        message.setContent(JSONObject.toJSONString(content));

        messageService.addMessage(message);
    }

    /*消费发帖事件*/
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        /*recode接收相关数据*/
        if(record == null || record.value() == null){
            logger.error("消息的内容为空");
            return;
        }
        /*将接收的消息反序列化*/
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        /*查询出帖子*/
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());

        /*将帖子发送到elasticsearch服务器上*/
        elasticsearchService.saveDiscussPost(post);

    }
    /*消费删帖事件*/
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        /*recode接收相关数据*/
        if(record == null || record.value() == null){
            logger.error("消息的内容为空");
            return;
        }
        /*将接收的消息反序列化*/
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        /*将帖子发送到elasticsearch服务器上*/
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    /*消费分享事件*/
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record){
        /*recode接收相关数据*/
        if(record == null || record.value() == null){
            logger.error("消息的内容为空");
            return;
        }
        /*将接收的消息反序列化*/
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/"
                + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        /*生产者产生的消息可以被多个消费组接收，但是一个消费组中只有一个消费者可以消费消息
        * 所以在消费者消费事件中使用spring Schedule定时任务不会产生分布式的问题*/
        /*启动定时器，监视该图片，一旦生成，则上传到云服务器*/

        UploadTask uploadTask = new UploadTask(fileName,suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask, 500);
        uploadTask.setFuture(future);

    }

    class UploadTask implements Runnable{

        /*文件名称*/
        private String fileName;

        /*文件后缀*/
        private String suffix;

        /*启动任务的返回值*/
        private Future future;

        /*开始时间*/
        private long startTime;

        /*上传次数*/
        private int uploadTimes;

        public void setFuture(Future future) {
            this.future = future;
        }

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {

            /*生成图片失败*/
            if(System.currentTimeMillis() - startTime > 30000){
                logger.error("执行任务事件过长，终止任务: " + fileName);
                /*停止定时器*/
                future.cancel(true);
                return;
            }
            if(uploadTimes >= 3){
                logger.error("上传次数过多，终止任务: " + fileName);
                /*停止定时器*/
                future.cancel(true);
                return;
            }

            /*本地存放文件路径*/
            String path = wkImageStorage + "/" + fileName + suffix;

            File file = new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s]", ++uploadTimes, fileName));
                /*设置响应信息*/
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                /*生成上传凭证*/
                Auth auth = Auth.create(accessKey,secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);

                /*指定上传机房*/
                UploadManager manager = new UploadManager(new Configuration(Region.huanan()));
                try {
                    /*开始上传图片*/
                    Response response =manager.put(
                            path,fileName,uploadToken,null,"image/" + suffix,false
                    );
                    /*处理响应结果*/
                    JSONObject json = JSONObject.parseObject(response.bodyString());

                    if(json == null || json.get("code") == null || !json.get("code").toString().equals("0")){
                        logger.info(String.format("第%d次上传失败[%s]", uploadTimes, fileName));
                    }else {
                        logger.info(String.format("第%d次上传成功[%s]", uploadTimes, fileName));
                        future.cancel(true);
                    }

                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s]", uploadTimes, fileName));
                }

            }else {
                logger.info("等待图片生成[" + fileName + "]...");
            }
        }
    }
}
