package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
//@Scope("prototype")
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
//        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init() {
//        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    public void destroy() {
//        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }

    //事务传播机制
    //REQUIRED：支持当前事务（外部事务），比如 A 事务调用 B 事务，B 事务以 A 事务的事务为标准，如果 A 不存在事务则创建一个新的事务
    //REQUIRED_NEW：创建一个新事务，按照 B 事务的标准执行，不管 A 是否有事务，如果有事务暂定当前事务（外部事务）即 A 事务；
    //NESTED：如果当前存在事务（外部事务），则嵌套在该事务中执行，即如果 A 有事务，B 事务有独立的提交和回滚，如果 A 没有事务则创建一个新的事务，和 REQUIRED 一样。
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1(){
        //新增用户
        User user = new User();
        user.setUsername("bate");
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("bate@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        //用新增用户发帖
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setCreateTime(new Date());
        post.setTitle("新人报道");
        post.setContent("你好");
        discussPostMapper.insertDiscussPost(post);

        int i = 1/0;
        return "ok";
    }

    /**
     * 编程式事务
     */
    public Object save2(){
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                //新增用户
                User user = new User();
                user.setUsername("alpha");
                user.setSalt(CommunityUtil.generateUUID().substring(0,5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("123@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);
                //用新增用户发帖
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setCreateTime(new Date());
                post.setTitle("新人报道");
                post.setContent("hello");
                discussPostMapper.insertDiscussPost(post);

                int i = 1/0;
                return "ok";
            }
        });
    }
}
