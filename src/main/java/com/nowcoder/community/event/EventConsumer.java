package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息的消费者(kafka)
 * 别动触发
 */
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

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
}
