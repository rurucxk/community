package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLogin(){
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已经向你的邮箱发送了一封激活邮件，请在30分钟内激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }

    }

    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("code") String code){
        int result = userService.activation(userId, code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg","激活成功，您的账号已经可以正常操作");
            model.addAttribute("target","/login");
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","无效操作，您的账号已经可以激活过了");
            model.addAttribute("target","/index");
        }else {
            model.addAttribute("msg","激活失败，您提供的激活码不正确");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

    /**
     * 使用redis存储验证码，不在将验证码注入session中
     * @param response
     */
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/)  {
        //生成验证码
        String text = kaptchaProducer.createText();
        //生成验证码图片
        BufferedImage image = kaptchaProducer.createImage(text);

/*        //将验证码存入session
        session.setAttribute("kaptcha", text);*/

        /*
            存入redis
            验证码的归属
         */
        /*cookie设置*/
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        /*验证码存入redis*/
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        /*设置60秒过期*/
        redisTemplate.opsForValue().set(kaptchaKey,text,60, TimeUnit.SECONDS);


        //将图片输出给服务器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    /**
     *不在从session中去验证码，而是从redis中取
     * 从cookie中取临时凭证生成redis的key
     * required = false,cookieValue注解默认是必须要取到值，其中required属性默认为true，如果过期后就取不到值，
     * 但是又因为需要值因此会直接报错不会赋空值 加个属性就行改为false 即能接受空值
     */
    @PostMapping("/login")
    public String login (Model model/*, HttpSession session*/,
                         HttpServletResponse response,
                         String username, String password,
                         String code, boolean rememberme,
                         @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner){
        /*String kaptcha = (String) session.getAttribute("kaptcha");*/
        String kaptcha = null;
        /*判断是否失效*/
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        //验证码比较
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg", "验证码错误");
            return "/site/login";
        }
        //验证账号，密码
            //超时时间
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            //重定向index
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));

            return "/site/login";
        }

    }

    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }

    @GetMapping("/forget")
    public String getForgetPage(){
        return "/site/forget";
    }

    @GetMapping("/forget/code")
    @ResponseBody
    public String getCode(String email, HttpSession session)  {
        if (StringUtils.isBlank(email)) {
            return CommunityUtil.getJSONString(1, "邮箱不能为空！");
        }
        Map<String, Object> map = userService.verifyEmail(email);
        //判断是否有邮箱的注册信息
        if(map.containsKey("user")) {
            // 保存验证码
            session.setAttribute("verifyCode", map.get("code"));
            return CommunityUtil.getJSONString(0);
        } else {
            return CommunityUtil.getJSONString(1, "查询不到该邮箱注册信息");
        }
    }

    @PostMapping("/forget/password")
    public String resetPassword(Model model, HttpSession session,
                                String email, String password,String verifyCode){
        String code = (String) session.getAttribute("verifyCode");
        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg", "验证码错误!");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) {
            model.addAttribute("msg","修改密码成功，请重新登录");
            model.addAttribute("target","/login");
            return "redirect:/login";
        }
        else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}
