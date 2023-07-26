package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @LoginRequired
    @GetMapping("/setting")
    public String getSetting(Model model){

        /*生成上传文件名*/
        String fileName = CommunityUtil.generateUUID();

        /*设置云服务器响应信息*/
        StringMap policy = new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));

        /*生成上传到云服务器的凭证*/
        Auth auth = Auth.create(accessKey,secretKey);
        String uploadToken =auth.uploadToken(headerBucketName,fileName,3600,policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);

        return "/site/setting";
    }

    /**
     * 更新头像路径，将云服务器上传的头像，保存到数据库中
     */
    @PostMapping("/header/url")
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    /**
     * 上传头像
     * 弃用
     * 现使用云服务器上传
     */
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }

        //获取后缀名
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf('.'));

        //判断是否有后缀格式
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error", "请选择正确格式的图片");
            return "/site/setting";
        }

        //生成随机文件名+后缀
        filename = CommunityUtil.generateUUID() + suffix;
        //确定文件存放位置
        File dest = new File(uploadPath + "/" + filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常！" + e);
        }

        //更新用户头像的路径(web访问路径) -> http//localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";
    }

    /**
     * 获取头像
     * 弃用
     * 从云服务器中获取
     */
    @GetMapping("/header/{filename}")
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //服务器存放图片路径
        filename = uploadPath + "/" + filename;

        //获取文件后缀名
        String suffix = filename.substring(filename.lastIndexOf('.'));

        //响应图片
        response.setContentType("/image" + suffix);
        try (
                //java7的特性，在这个括号里写了的会在生成finally中调用close方法，如果有的话
                //需要关闭手动声明的输入输出流
                OutputStream os = response.getOutputStream();
                BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(filename));
                ){
            byte[] kb = new byte[1024];
            //代表实际读取的字节数
            int length = 0;
            while ((length = buffer.read(kb)) != -1) {
                os.write(kb, 0, length);
            }

        } catch (IOException e) {
            logger.error("读取头像失败" + e.getMessage());
        }
    }

    @PostMapping("/updatePassword")
    public String updatePassword(String oldPassword, String newPassword,
                                 String confirmPassword, Model model){
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user, oldPassword, newPassword, confirmPassword);
        if(!map.isEmpty()){
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
        return "redirect:/logout";
    }

    /**
     * 个人主页
     */
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new IllegalArgumentException("用户不存在");
        }

        /*
            将用户的信息传给页面
         */
        model.addAttribute("user", user);
        /*
            用户收到赞的数量
         */
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);

        /*
            关注了人的数量
         */
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        /*
            粉丝数量
         */
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);

        /*
            是否关注
         */
        boolean hasFollowee = false;
        if (hostHolder.getUser() != null) {
            hasFollowee = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowee",hasFollowee);

        return "/site/profile";
    }
    /**
     * 查询用户发布的帖子
     */
    @GetMapping("/myPosts/{userId}")
    public String getMyPosts(@PathVariable("userId") int userId, Page page, Model model){
        /*
            查询当前需要的用户
         */
        User user = userService.findUserById(userId);
        if(user == null){
            throw new IllegalArgumentException("用户不存在");
        }
        model.addAttribute("user", user);
        /*
            帖子数量
         */
        int postRows = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postRows", postRows);
        /*
            分页设置
         */
        page.setPath("/user/myPosts/" + userId);
        page.setLimit(5);
        page.setRows(postRows);
        /*
            用户发布的帖子
         */
        List<Map<String, Object>> postList = new ArrayList<>();
        List<DiscussPost> posts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(),0);
        if(posts != null){
            for (DiscussPost post : posts) {
                Map<String, Object> map = new HashMap<>();
                map.put("post",post);
                /*
                    帖子的赞
                 */
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                postList.add(map);
            }
        }
        model.addAttribute("posts", postList);
        return "/site/my-post";
    }
    
    @GetMapping("/myReplies/{userId}")
    public String getMyReplies(@PathVariable("userId") int userId, Page page, Model model) {
        /*
            查询当前需要的用户
         */
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        model.addAttribute("user", user);
        /*
            回复帖子数量
         */
        int replyCount = commentService.findUserPostReplyCount(userId);
        model.addAttribute("replyCount",replyCount);
        /*
            分页设置
         */
        page.setPath("/user/myReplies/" + userId);
        page.setLimit(5);
        page.setRows(replyCount);

        /*
            用户对帖子回复
         */
        List<Map<String, Object>> replyList = new ArrayList<>();
        List<Comment> postReplies = commentService.findUserPostReply(userId, page.getOffset(), page.getLimit());

        if(postReplies != null){
            for (Comment reply : postReplies) {
                Map<String, Object> map = new HashMap<>();
                map.put("reply", reply);
                map.put("post", discussPostService.findDiscussPostById(reply.getEntityId()));
                replyList.add(map);
            }

        }
        model.addAttribute("replies", replyList);


        return "/site/my-reply";
    }
}
