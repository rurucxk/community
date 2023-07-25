package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 对每一个请求就行记录来统计独立访客和活跃用户
 */
@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dateService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*统计UV*/
        /*获取ip*/
        String ip = request.getRemoteHost();
        dateService.recordUV(ip);

        /*统计DAU*/
        User user = hostHolder.getUser();
        if(user != null){
            dateService.recordDAU(user.getId());
        }
        return true;
    }
}
