package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Redis的UV和DAU数据统计
 */
@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    /*格式化日期*/
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    /*将指定的ip计入UV HyperLogLog*/
    public void recordUV(String ip){
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));

        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    /*统计指定日期范围内的UV HyperLogLog*/
    public long calculateUV(Date start, Date end){
        if(start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(start.after(end)){
            throw new IllegalArgumentException("请输入正确的开始日期");
        }

        /*整理该日期范围内的key*/
        List<String> keyList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        /*遍历日期
        * calender得到的日期早于end就循环
        * !calendar.getTime().after(end) == calendar.getTime().before(end) */
        while (!calendar.getTime().after(end)){

            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);

            /*日期加一*/
            calendar.add(Calendar.DATE,1);
        }

        /*合并这些数据*/
        String redisKey = RedisKeyUtil.getUVKey(df.format(start),df.format(end));

        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        /*返回统计的结果*/
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    /*将指定用户计入DAU中 BitMap*/
    public void recordDAU(int userId){
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));

        redisTemplate.opsForValue().setBit(redisKey,userId,true);
    }

    /*查询指定日期范围的DAU bitMap*/
    public long calenderDAU(Date start, Date end){
        if(start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(start.after(end)){
            throw new IllegalArgumentException("请输入正确的开始日期");
        }
        /*整理该日期范围内的key*/
        List<byte[]> keyList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        /*遍历日期
         * calender得到的日期早于end就循环
         * !calendar.getTime().after(end) == calendar.getTime().before(end) */
        while (!calendar.getTime().after(end)){

            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());

            /*日期加一*/
            calendar.add(Calendar.DATE,1);
        }

        /*进行OR运算，只要日期范围内的任意一天活跃过就算活跃用户*/
        String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
        return (long) obj;

    }
}
