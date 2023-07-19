package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;

import javax.swing.text.Style;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTest {

    /**
     * RedisTemplate注入2方式
     */
//    @Qualifier("redisTemplate")
//    @Autowired
//    private RedisTemplate template;

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
        String key =  "test:count";

        redisTemplate.opsForValue().set(key, 2);
        System.out.println(redisTemplate.opsForValue().get(key));
        System.out.println(redisTemplate.opsForValue().increment(key));
        System.out.println(redisTemplate.opsForValue().decrement(key));
    }

    @Test
    public void testHashes(){
        String key = "test:hash";

        redisTemplate.opsForHash().put(key,"id", 1);
        redisTemplate.opsForHash().put(key,"name", "张三");
        System.out.println(redisTemplate.opsForHash().get(key, "id"));
        System.out.println(redisTemplate.opsForHash().get(key, "name"));
    }

    @Test
    public void testLists(){
        String key = "test:list";

        redisTemplate.opsForList().leftPush(key,1);
        redisTemplate.opsForList().leftPush(key,2);
        redisTemplate.opsForList().leftPush(key,3);
        redisTemplate.opsForList().leftPush(key,0);
        System.out.println(redisTemplate.opsForList().size(key));
        System.out.println(redisTemplate.opsForList().index(key, 0));
        System.out.println(redisTemplate.opsForList().range(key,0,2));
        System.out.println(redisTemplate.opsForList().leftPop(key));
        System.out.println(redisTemplate.opsForList().rightPop(key));

    }

    @Test
    public void testSets(){
        String key = "test:set";

        redisTemplate.opsForSet().add(key,12,13,14,15,16,11,10,20);
        System.out.println(redisTemplate.opsForSet().size(key));
        System.out.println(redisTemplate.opsForSet().members(key));
        System.out.println(redisTemplate.opsForSet().randomMember(key));
        System.out.println(redisTemplate.opsForSet().pop(key,2));
        System.out.println(redisTemplate.opsForSet().size(key));

    }

    @Test
    public void testSortSet(){
        String key = "test:sortSet";

        redisTemplate.opsForZSet().add(key,"张三", 10);
        redisTemplate.opsForZSet().add(key,"李四", 20);
        redisTemplate.opsForZSet().add(key,"王五", 30);
        redisTemplate.opsForZSet().add(key,"麻六", 1);
        redisTemplate.opsForZSet().add(key,"小七", 6);

        System.out.println(redisTemplate.opsForZSet().zCard(key));
//        System.out.println(redisTemplate.opsForZSet().score(key));
        System.out.println(redisTemplate.opsForZSet().score(key,"张三"));
        System.out.println(redisTemplate.opsForZSet().rank(key,"小七"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(key,"小七"));
        System.out.println(redisTemplate.opsForZSet().range(key,0, 4));
        System.out.println(redisTemplate.opsForZSet().reverseRange(key,0, 4));

    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:set2");

        System.out.println(redisTemplate.hasKey("test:set2"));

        redisTemplate.expire("test:set", 10, TimeUnit.SECONDS);
    }

    /**
     * 多次访问同一个key
     */
    @Test
    public void testBoundOperations(){
        String key = "test:count";
        BoundValueOperations valueOperations = redisTemplate.boundValueOps(key);
        BoundHashOperations hashOperations = redisTemplate.boundHashOps("test:hash");

        System.out.println(valueOperations.get());
        valueOperations.increment();
        valueOperations.increment();
        valueOperations.increment();
        valueOperations.decrement();
        System.out.println(valueOperations.get());
    }

    /**
     * 编程式事务
     */
    @Test
    public void testTransactional(){
        System.out.println(redisTemplate.opsForSet().members("test:tx"));
        Object execute = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String key = "test:tx";

                operations.multi();
                operations.opsForSet().add(key,"张三","李四","王五");
                operations.opsForSet().add(key,"麻六","张三");
                System.out.println(operations.opsForSet().members(key));
                return operations.exec();
            }
        });
        System.out.println(execute);
    }
}
