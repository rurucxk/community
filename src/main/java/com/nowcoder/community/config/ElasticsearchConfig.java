package com.nowcoder.community.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch设置
 */
@Configuration
public class ElasticsearchConfig {

    /*将Elasticsearch的传入*/
    @Value("${spring.elasticsearch.uris}")
    private String esUrl;

}
