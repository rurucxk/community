package com.nowcoder.community.util;

import lombok.Data;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


/**
 * 敏感词过滤器
 * 1.定义前缀树
 * 2.根据敏感词，初始化树
 * 3.编写过滤敏感词的方法
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //替换符
    private static final String REPLACEMENT ="***";

    //根节点
    private TrieNode rootNode = new TrieNode();

    //注解：当前类被实例化后会自动调用被注解的方法
    @PostConstruct
    public void init(){
        //通过类加载器来获取文件(在target目录下获取)
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyWord;
            while ((keyWord = reader.readLine()) != null){
                //添加前缀树
                this.addKeyWord(keyWord);
            }
        }catch(IOException e){
            logger.error("加载敏感词文件失败" + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加到前缀树中
     */
    private void addKeyWord(String keyWord){
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyWord.length(); i++) {
            char c= keyWord.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if(subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            //指向子节点
            tempNode = subNode;

            //设置结束的标识
            if(i == keyWord.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }

        //指针1(树指针)
        TrieNode tempNode =rootNode;

        //指针2(头指针)
        int begin = 0;

        //指针3(尾指针)
        int position = 0;

        //结果
        StringBuilder sb = new StringBuilder();

        while (begin < text.length()){
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点，将此符号计入结果，让指针2向下走
                if(tempNode == rootNode){
                    sb.append(c);
                    begin++;
                }

                //无论指针1在哪，指针3都会向下走
                position++;
                continue;
            }
            //检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头的字符不是敏感词
                sb.append(text.charAt(begin));
                //进入下一个位置
                begin++;
                position = begin;
                //重新指向根节点
                tempNode = rootNode;
            }else if(tempNode.isKeywordEnd()){
                //发现敏感词，将begin到position的字符串替换掉
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin = ++position;
                //重新指向根节点
                tempNode = rootNode;
            }else if(position < text.length() - 1){
                //检查下一个字符
                position++;
            }else {
                position = begin;
            }
        }
        //将最后一批字符计入结果
        sb.append(text.substring(begin, position));
        return sb.toString();

    }

    /**
     * 判断是否为符号
     */
    private boolean isSymbol(Character c){
        //(isAsciiAlphanumeric)判断是否合法的普通字符        东亚文字的范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 前缀树
     */
    @Data
    private class TrieNode {

        //关键词结束的标识
        private boolean isKeywordEnd =false;

        //子节点(key是下级字符，value是下级节点)
        private Map<Character, TrieNode> subNode = new HashMap<>();

        //添加子节点
        private void addSubNode(Character c, TrieNode node){
            subNode.put(c,node);
        }

        //获取子节点
        private TrieNode getSubNode(Character c){
            return subNode.get(c);
        }
    }
}
