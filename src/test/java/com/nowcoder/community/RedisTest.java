package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;

import javax.swing.text.Style;
import java.util.Random;
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

    /*统计20万个重复的数据的独立数目，使用HyperLogLog*/
    @Test
    public void testHyperLogLog(){
        String key = "test:hll:01";
        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(key,i);
        }
        for (int i = 0; i < 100000; i++) {
           int r = (int) (Math.random() * 10000 + 1);
            redisTemplate.opsForHyperLogLog().add(key, r);
        }

        System.out.println(redisTemplate.opsForHyperLogLog().size(key));
    }

    /*将3组数据合并，再统计合并后的重复数据的独立数据*/
    @Test
    public void testHyperLogLogUnion(){
        String key2 = "test:hll:02";
        for (int i = 0; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(key2,i);
        }
        String key3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(key3,i);
        }
        String key4 = "test:hll:02";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(key4,i);
        }
        /*数据合并*/
        String unionKey ="test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey,key2,key3,key4);
        System.out.println(redisTemplate.opsForHyperLogLog().size(unionKey));
    }

    /*统计一组数据的布尔值
    * BitMap不是独立的数据结构，是特殊的string类型，也就是byte[]*/
    @Test
    public void testBitMap(){
        String key = "test:bm:01";

        /*记录，默认为false*/
        redisTemplate.opsForValue().setBit(key,1,true);
        redisTemplate.opsForValue().setBit(key,4,true);
        redisTemplate.opsForValue().setBit(key,7,true);

        /*查询*/
        System.out.println(redisTemplate.opsForValue().getBit(key, 0));
        System.out.println(redisTemplate.opsForValue().getBit(key, 1));
        System.out.println(redisTemplate.opsForValue().getBit(key, 2));
        System.out.println(redisTemplate.opsForValue().getBit(key, 7));

        /*统计*/
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(key.getBytes());
            }
        });
        System.out.println(obj);
    }

    /*统计3组数据的布尔值，并对这3组数据做OR运算*/
    @Test
    public void testBitMapOperation(){
        String key2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(key2,0,true);
        redisTemplate.opsForValue().setBit(key2,1,true);
        redisTemplate.opsForValue().setBit(key2,2,true);

        String key3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(key3,2,true);
        redisTemplate.opsForValue().setBit(key3,3,true);
        redisTemplate.opsForValue().setBit(key3,4,true);

        String key4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(key4,4,true);
        redisTemplate.opsForValue().setBit(key4,5,true);
        redisTemplate.opsForValue().setBit(key4,6,true);

        String orKey = "test:bm:or";

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        orKey.getBytes(),
                        key2.getBytes(), key3.getBytes(), key4.getBytes());
                return connection.bitCount(orKey.getBytes());
            }
        });

        System.out.println(obj);

        System.out.println(redisTemplate.opsForValue().getBit(orKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 5));
        System.out.println(redisTemplate.opsForValue().getBit(orKey, 6));

        System.out.println(redisTemplate.opsForValue().size(orKey));


    }
}
