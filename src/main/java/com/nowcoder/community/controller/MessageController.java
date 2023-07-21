package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    /**
     * 私信列表
     */
    @GetMapping("/letter/list")
    @LoginRequired
    public String getLetterList(Model model, Page page){

        User user = hostHolder.getUser();
        /**
         * 设置分页信息
         */
        page.setPath("/letter/list");
        page.setLimit(5);
        page.setRows(messageService.findConversationCount(user.getId()));

        /**
         * 会话列表
         */
        List<Message> conversationsList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());

        List<Map<String, Object>> conversations = new ArrayList<>();
        if(!conversationsList.isEmpty()){
            for (Message message : conversationsList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation",message);
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target",userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);
        /*查询所有未读私信*/
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        /*查询未读系统通知的数量*/
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/letter";
    }

    /**
     * 私信详情
     */

    @GetMapping("/letter/detail/{conversationId}")
    @LoginRequired
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model){
        /*
          分页设置
         */
        page.setPath("/letter/detail/" + conversationId);
        page.setLimit(5);
        page.setRows(messageService.findLetterCount(conversationId));

        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if(!letterList.isEmpty()){
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
//                map.put("toUser", userService.findUserById(message.getToId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);
        /*
           私信目标
         */
        model.addAttribute("target",getLetterTarget(conversationId));

        /*
            将未读的私信设置为已读
         */
        List<Integer> ids = getLetterIds(letterList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }


        return "/site/letter-detail";

    }

    /**
     * 获取私信的用户
     */
    private User getLetterTarget(String conversationId){
        String[] s = conversationId.split("_");
        int id0 = Integer.parseInt(s[0]);
        int id1 = Integer.parseInt(s[1]);

        if(hostHolder.getUser().getId() == id0){
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    /**
     * 获取未读私信的id
     */
    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> ids = new ArrayList<>();

        if(!letterList.isEmpty()){
            for (Message message : letterList) {
                if(hostHolder.getUser().getId() == message.getToId() && message.getStatus() ==0){
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    /**
     * 异步发送私信
     */
    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter(String toName, String content){
        User target = userService.findUserByName(toName);
        if(target == null){
            return CommunityUtil.getJSONString(1,"目标用户不存在");
        }

        /*
            消息设置
         */
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if(message.getFromId() < message.getToId()){
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        }else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());

        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);

    }

    /**
     * 删除消息
     */
    @PostMapping("/letter/delete")
    @ResponseBody
    public String deleteLetter(int id){
        if(id == 0){
            return CommunityUtil.getJSONString(1,"消息不存在");
        }
        messageService.deleteMessage(id);

        return CommunityUtil.getJSONString(0);
    }

    /**
     * 系统通知列表
     */
    @GetMapping("/notice/list")
    public String getNotice(Model model){
        User user = hostHolder.getUser();

        /*评论的通知*/
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if(message !=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            /*将序列化的系统通知的content反序列化*/
            /*将已转义的content还原*/
            String content = HtmlUtils.htmlUnescape(message.getContent());
            /*将content转为对象*/
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));

            /*通知数量*/
            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count", count);

            /*未读通知的数量*/
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unreadCount",unreadCount);
            model.addAttribute("commentNotice",messageVo);
        }


        /*点赞的通知*/
        message = messageService.findLatestNotice(user.getId(), TOPIC_LiKE);
        if(message !=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            /*将序列化的系统通知的content反序列化*/
            /*将已转义的content还原*/
            String content = HtmlUtils.htmlUnescape(message.getContent());
            /*将content转为对象*/
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));

            /*通知数量*/
            int count = messageService.findNoticeCount(user.getId(), TOPIC_LiKE);
            messageVo.put("count", count);

            /*未读通知的数量*/
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LiKE);
            messageVo.put("unreadCount",unreadCount);
            model.addAttribute("likeNotice",messageVo);
        }


        /*关注的通知*/
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if(message !=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            /*将序列化的系统通知的content反序列化*/
            /*将已转义的content还原*/
            String content = HtmlUtils.htmlUnescape(message.getContent());
            /*将content转为对象*/
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));

            /*通知数量*/
            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count", count);

            /*未读通知的数量*/
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unreadCount",unreadCount);
            model.addAttribute("followNotice",messageVo);
        }


        /*未读私信的数量*/
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        /*未读系统通知的数量*/
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/notice";
    }

    /**
     * 通知详情
     * @param topic
     * @param page
     * @param model
     * @return
     */
    @GetMapping("/notice/detail/{topic}")
    public String getNotices(@PathVariable("topic") String topic, Page page, Model model){
        User user = hostHolder.getUser();

        page.setPath("/notice/detail/" + topic);
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();

        if(!noticeList.isEmpty()){
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                /*通知*/
                map.put("notice", notice);
                /*内容*/
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user",userService.findUserById((Integer) data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));

                /*通知的作者*/
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        /*设置已读*/
        List<Integer> ids = getLetterIds(noticeList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }


}
