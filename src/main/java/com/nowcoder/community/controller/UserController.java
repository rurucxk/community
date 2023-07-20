package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @LoginRequired
    @GetMapping("/setting")
    public String getSetting(){
        return "/site/setting";
    }

    /**
     * 上传头像
     * @param headerImage
     * @param model
     * @return
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
     * @param filename
     * @param response
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

        return "/site/profile";
    }
}
