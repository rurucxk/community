package com.nowcoder.community.entity;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import java.util.Date;

@Data
/*通过注解连接上es*/
@Document(indexName = "discusspost")
public class DiscussPost {

    @Id
    private int id;

    @Field(type = FieldType.Integer)
    private int userId;

    /*互联网校招
    * analyzer:存储时为了存储更多的词，使用ik的max尽可以的拆出更多的词
    *searchAnalyzer：搜索的时候为了更好的匹配用户的意思，使用ik的smart来匹配用户的想法 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    /*回复帖子的数量*/
    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Double)
    private double score;
}
