package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements UserDetailsService, CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

/*    @Autowired
    private LoginTicketMapper loginTicketMapper;*/

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        /*return userMapper.selectById(id);*/
        /*
            1.先从cache中查询
         */
        User user = getCache(id);
        if (user == null){
            user = initCache(id);
        }
        return user;
    }

    public User findUserByName(String name){
        return userMapper.selectByName(name);
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u !=null){
            map.put("usernameMsg","账号已存在");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null){
            map.put("emailMsg","邮箱已存在");
            return map;
        }
        //账号注册
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        userMapper.insertUser(user);

        //激活邮件
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(),"激活账号", content);
        return map;

    }

    public int activation(int userId, String code){
        User user=userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }
        else {
            return ACTIVATION_FAILURE;
        }
    }

    //登录
    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (username == null){
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (password == null){
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if(user == null){
            map.put("usernameMsg", "该账号不存在");
            return map;
        }
        //验证是否激活
        if(user.getStatus() == 0){
            map.put("usernameMsg", "该账号未激活");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "请输入正确的密码");
            return map;
        }

        //生成登录凭证
//        LoginTicket loginTicket = new LoginTicket(user.getId(),
//                0,
//                CommunityUtil.generateUUID(),
//                new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L));
/*        loginTicketMapper.insertLoginTicket(loginTicket);*/
        /*改为存入redis*/
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        /*将loginTicket对象存入redis的String中，redis会自动的将对象序列化*/
        redisTemplate.opsForValue().set(ticketKey,loginTicket);


        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    //登出
    /*更新ticket的状态*/
    public void logout(String ticket){
        /*loginTicketMapper.updateStatus(ticket,1);*/
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    //根据凭证查询
    public LoginTicket findLoginTicket(String ticket){
        /*LoginTicket loginTicket = loginTicketMapper.selectByTicket(ticket);*/
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    //更新用户头像
    public int updateHeader(int userId, String headerUrl){
        int header = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return header;
    }

    //重置密码前校验邮箱
    public Map<String, Object> verifyEmail(String email) {
        Map<String,Object> map = new HashMap<>();
        // 空值处理
        if (StringUtils.isBlank(email)) {
            //不需要写emailMsg，直接返回空的就行
            return map;
        }
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            //不需要写emailMsg，直接返回空的就行
            return map;
        } else {
            //如果能查到这个邮箱就发送邮件
            Context context = new Context();
            context.setVariable("email", email);
            String code = CommunityUtil.generateUUID().substring(0, 4);
            context.setVariable("verifyCode", code);
            String content = templateEngine.process("/mail/forget", context);
            mailClient.sendMail(email, "找回密码", content);
            map.put("code", code);
        }
        map.put("user", user);
        return map;
    }

    //重置密码
    public Map<String, Object> resetPassword(String email, String password){
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(email)){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        // 验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }
        //验证密码是否与原密码一致
        password = CommunityUtil.md5(password + user.getSalt());
//        map.put("passwordMsg",password);
        System.out.println(user.getPassword());
        if(password.equals(user.getPassword())){
            map.put("passwordMsg","密码不能与原密码一致");
            return map;
        }
        userMapper.updatePassword(user.getId(),password);
        clearCache(user.getId());
        map.put("user", user);
        return map;
    }

    //更改用户密码
    public Map<String, Object> updatePassword(User user, String oldPassword,String newPassword, String confirmPassword){
        Map<String, Object> map = new HashMap<>();

        if(StringUtils.isBlank(oldPassword) ){
            map.put("oldPasswordMsg","原密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(newPassword) ){
            map.put("newPasswordMsg","密码不能为空");
            return map;
        }
        //原密码判断
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if(!oldPassword.equals(user.getPassword())){
            map.put("oldPasswordMsg","密码不正确");
            return map;
        }else{
            newPassword = CommunityUtil.md5(newPassword + user.getSalt());
            userMapper.updatePassword(user.getId(),newPassword);
            clearCache(user.getId());
        }
        return map;
    }

    /*1.优先从redis缓存中查询user
    * 2.取不到时初始化缓存数据
    * 3.当数据变更时先删除缓存，再跟新缓存*/

    /*优先从redis缓存中查询user*/
    private User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    /*取不到时初始化缓存数据*/
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        /*一个小时过期*/
        redisTemplate.opsForValue().set(userKey,user, 3600, TimeUnit.SECONDS);
        return user;
    }

    /*当数据变更时先删除缓存，惰性的更新数据，当查询的时候才会从数据库中缓存新数据*/
    public void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

//    /*获取用户权限*/
//    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
//        User user = this.findUserById(userId);
//
//        List<GrantedAuthority> list = new ArrayList<>();
//        list.add(new GrantedAuthority() {
//            @Override
//            public String getAuthority() {
//                /*权限判断*/
//                switch (user.getType()){
//                    case 1:
//                        return AUTHORITY_ADMIN;
//                    case 2:
//                        return AUTHORITY_MODERATOR;
//                    default:
//                        return AUTHORITY_USER;
//                }
//            }
//        });
//        return list;
//    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.findUserByName(username);
    }
}
