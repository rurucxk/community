package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.ElasticsearchService;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;


//    @Autowired
//    private ElasticsearchOperations operations;

    /*遇到问题，ElasticsearchTemplate无法自动注入，
    原因：Spring boot在集成7.X版本的时候已经弃用了ElasticsearchTemplate，
    解决方法：改为ElasticsearchRestTemplate。
    ElasticsearchRestTemplate为ElasticsearchOperations的实现子类*/
    @Autowired
    private ElasticsearchRestTemplate template;

    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPsotById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPsotById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPsotById(243));
    }

    @Test
    public void testInsertList(){
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(101, 0, 100);
        System.out.println(list);
        discussPostRepository.saveAll(list);
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134,0,100));
    }

    @Test
    public void testUpdate(){
        DiscussPost post = discussPostMapper.selectDiscussPsotById(231);
        post.setContent("我是新人，水水水");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete(){
        discussPostRepository.deleteById(231);
        discussPostRepository.deleteAll();
    }

    /*利用Repository搜索
    * withQuery:搜索条件
    * multiMatchQuery:多个字段同时匹配
    * SortBuilders:排序条件
    * Sort.by:指定按什么属性排序并指定正序还是倒序
    * withPageable:分页
    * PageRequest:分页条件
    * withHighlightFields:高亮文本,可指定多个
    * new HighlightBuilder.Field:指定哪些属性高亮，并设置前后标签*/
    @Test
    public void testSearchByRepository(){
        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(Sort.by(Sort.Direction.DESC,"type"))
                .withSort(Sort.by(Sort.Direction.DESC,"score"))
                .withSort(Sort.by(Sort.Direction.DESC,"createTime"))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        /*获取了高亮，但是没有返回*/
        /*查询结果*/
        SearchHits<DiscussPost> search = template.search(builder, DiscussPost.class);
        /*转为SearchPage对象来获取分页信息*/
        SearchPage<DiscussPost> page = SearchHitSupport.searchPageFor(search, builder.getPageable());
//        System.out.println(search.getTotalHitsRelation());
        /*查询到的总条数*/
        System.out.println(search.getTotalHits());
        System.out.println(page.getTotalElements());

        /*获取搜索结果的总页数*/
        System.out.println(page.getTotalPages());

        /*获取当前页的页码，从 0 开始计数*/
        System.out.println(page.getNumber());

        /*获取当前页的大小，即每页包含的搜索结果数量*/
        System.out.println(page.getSize());

        /*取搜索结果的排序信息，返回一个Sort对象*/
        System.out.println(page.getSort());

        search.forEach(System.out::println);
    }

    /*使用Template*/
    @Test
    public void testSearchByTemplate(){
        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(Sort.by(Sort.Direction.DESC,"type"))
                .withSort(Sort.by(Sort.Direction.DESC,"score"))
                .withSort(Sort.by(Sort.Direction.DESC,"createTime"))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //下面是封装将高亮部分成一个search对象，也可以不做，直接discussPostSearchHit.getHighlightFields()获取
        SearchHits<DiscussPost> search = template.search(builder, DiscussPost.class);
        List<DiscussPost> list = new ArrayList<>();
        if(search.getTotalHits() > 0){
            for (SearchHit<DiscussPost> hit : search) {
                /*获取命中对象*/
                DiscussPost post = hit.getContent();
                /*hit.getHighlightFields():获取高亮字段的映射返回一个 Map<String, List<String>>
                * 其中键是高亮字段的名称，值是高亮结果的列表
                * 如果title存在高亮字段就将其替换之前的title,多个高亮取第一个*/
                Map<String, List<String>> fields = hit.getHighlightFields();
                if(!fields.isEmpty()) {
                    if (fields.get("title") != null) {
                        post.setTitle(fields.get("title").get(0));
                    }
                    /*如果content存在高亮字段就将其替换之前的content*/
                    if (fields.get("content") != null) {
                        post.setContent(fields.get("content").get(0));
                    }
                }
                list.add(post);
            }
        }else {
            return;
        }

        /*将得到的list封装成page*/
        PageImpl<DiscussPost> pageInfo = new PageImpl<DiscussPost>(list, builder.getPageable(), search.getTotalHits());
        System.out.println(pageInfo.getTotalElements());
        pageInfo.forEach(System.out::println);
    }

    @Test
    public void testSearchDiscussPost(){
        SearchPage<DiscussPost> posts = elasticsearchService.searchDiscussPost("互联网", 0, 10);
        System.out.println(posts.getTotalElements());
        posts.forEach(System.out::println);
    }
}
