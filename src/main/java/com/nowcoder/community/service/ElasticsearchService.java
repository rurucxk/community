package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate template;

    /*向elasticsearch增加帖子*/
    public void saveDiscussPost(DiscussPost discussPost){
        discussPostRepository.save(discussPost);
    }

    /*删除elasticsearch中的帖子*/
    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }

    /**
     * 搜索elasticsearch中的帖子
     * keyword:关键字
     * current:分页条件，当前第几页
     *withQuery:搜索条件
     * multiMatchQuery:多个字段同时匹配
     * SortBuilders:排序条件
     * Sort.by:指定按什么属性排序并指定正序还是倒序
     * withPageable:分页
     * PageRequest:分页条件
     * withHighlightFields:高亮文本,可指定多个
     * new HighlightBuilder.Field:指定哪些属性高亮，并设置前后标签
     */

    public SearchPage<DiscussPost> searchDiscussPost(String keyword, int current, int limit){
        /*构造查询对象*/
        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword,"title","content"))
                .withSort(Sort.by(Sort.Direction.DESC,"type"))
                .withSort(Sort.by(Sort.Direction.DESC,"score"))
                .withSort(Sort.by(Sort.Direction.DESC,"createTime"))
                .withPageable(PageRequest.of(current,limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //下面是封装将高亮部分成一个search对象，也可以不做，直接discussPostSearchHit.getHighlightFields()获取
        SearchHits<DiscussPost> search = template.search(builder, DiscussPost.class);
        /*将SearchHits转为SearchPage*/
        SearchPage<DiscussPost> page = SearchHitSupport.searchPageFor(search, builder.getPageable());
        /*将高亮注入page*/
        if(!page.isEmpty()){
            for (SearchHit<DiscussPost> discussPostSearchHit : page) {
                /*获取post对象*/
                DiscussPost post = discussPostSearchHit.getContent();

                /*获取全部的高亮部分*/
                Map<String, List<String>> fields = discussPostSearchHit.getHighlightFields();

                if(!fields.isEmpty()){
                    /*获取全部的标题高亮部分*/
                    List<String> title = discussPostSearchHit.getHighlightFields().get("title");
                    if (title != null){
                        /*将高亮部分的title替换未高亮的title
                        * get(0)说明只高亮第一个被命中的关键词*/
                        post.setTitle(title.get(0));
                    }
                    /*获取全部高亮的内容部分*/
                    List<String> content = discussPostSearchHit.getHighlightFields().get("content");
                    if(content != null){
                        /*将高亮部分的content替换未高亮的content
                         * get(0)说明只高亮第一个被命中的关键词*/
                        post.setContent(content.get(0));
                    }
                }
            }
        }
        return page;

        /*用SearchHits<DiscussPost>去遍历SearchHit<DiscussPost> 返回的是PageImpl*/

        /*List<DiscussPost> list = new ArrayList<>();
        if(search.getTotalHits() > 0){
            for (SearchHit<DiscussPost> hit : search) {
                *//*获取命中对象*//*
                DiscussPost post = hit.getContent();
                *//*hit.getHighlightFields():获取高亮字段的映射返回一个 Map<String, List<String>>
                 * 其中键是高亮字段的名称，值是高亮结果的列表
                 * 如果title存在高亮字段就将其替换之前的title,多个高亮取第一个*//*
                Map<String, List<String>> fields = hit.getHighlightFields();
                if(!fields.isEmpty()) {
                    if (fields.get("title") != null) {
                        post.setTitle(fields.get("title").get(0));
                    }
                    *//*如果content存在高亮字段就将其替换之前的content*//*
                    if (fields.get("content") != null) {
                        post.setContent(fields.get("content").get(0));
                    }
                }
                list.add(post);
            }
        }
        return new PageImpl<DiscussPost>(list, builder.getPageable(), search.getTotalHits());*/


    }

}
