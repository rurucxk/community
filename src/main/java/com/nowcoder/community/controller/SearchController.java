package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch
 */
@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    /*search?keyword=xxx*/
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model){

        /*搜索帖子
        * page.getCurrent()默认从1开始*/
        SearchPage<DiscussPost> searchResult = elasticsearchService
                .searchDiscussPost(keyword, page.getCurrent() - 1/*错误 page.getOffset()*/, page.getLimit());

        /*聚合数据*/
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(searchResult != null){
            for (SearchHit<DiscussPost> discussPostSearchHit : searchResult){
                /*获取post对象*/
                DiscussPost post = discussPostSearchHit.getContent();
                Map<String, Object> map = new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        /*再页面上显示关键字keyword*/
        model.addAttribute("keyword",keyword);

        /*分页设置*/
        page.setPath("/search?keyword=" + keyword);
        /*获取总命中帖子的数量*/
        page.setRows(searchResult != null ? (int) searchResult.getTotalElements() : 0);

        return "/site/search";
    }
}
