package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

/*不推荐使用，已经使用redis存储登录凭证*/
@Deprecated
@Mapper
public interface LoginTicketMapper {


//    @Insert("insert into login_ticket (user_id, ticket, status, expired)" +
//            "values (#{userId}, #{ticket}, #{status}, #{expired})")
//    //自增主键自动生成，注入id，yml的设置对注解的sql不生效
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

//    @Select("select id, user_id, ticket, status, expired from login_ticket " +
//            "where ticket = #{ticket}")
    LoginTicket selectByTicket(String ticket);

//    @Update("update login_ticket set status = #{status} where ticket = #{ticket}")
    int updateStatus(String ticket, int status);


}
