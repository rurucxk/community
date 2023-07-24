package com.nowcoder.community.controller;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model,Page page){
//        方法调用前，SpringMVC会自动实例化 Model 和 Page，并将 Page 注入 Model。
//        所以，在 Thymeleaf 中可以直接访问 Page 对象中的数据
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        Object loginUSer = model.getAttribute("loginUSer");
        if(loginUSer != null){
            model.addAttribute("loginUser", loginUSer);
        }
        if(list != null){
            for (DiscussPost post : list) {
                Map<String,Object> map = new HashMap<>();
                map.put("post",post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                /*
                    查询帖子的赞
                 */
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }


        model.addAttribute("discussPosts", discussPosts);
        return "/index";
    }

    /**
     * 报错的页面
     */
    @GetMapping("/error")
    public String getErrorPage(){
        return "/error/500";
    }

    /**
     * 拒绝访问时的页面，就是无权访问时跳转的页面
     */
    @GetMapping("/denied")
    public String getDeniedPage(){
        return "/error/404";
    }

}
