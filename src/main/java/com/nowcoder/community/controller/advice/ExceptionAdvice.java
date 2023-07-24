package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 全局管理controller
 */
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    /**
        处理所有异常
     **/
    @SuppressWarnings("AlibabaRemoveCommentedCode")
    @ExceptionHandler({Exception.class})
    public void handlerException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常" + e.getMessage());
        /*
            异常信息
         */
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        /*
            判断请求是异步请求还是普通请求，异步请求要求返回json，普通请求要求返回页面
         */
        String xRequestedWith = request.getHeader("x-requested-with");
        /*
            XMLHttpRequest:异步请求
         */
        if("XMLHttpRequest".equals(xRequestedWith)){
//            response.setContentType("application/json");
            /*
                返回的是普通字符串，需要手动将其转为json
             */
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1,"服务器异常"));
        }else {
            response.sendRedirect(request.getContextPath() + "/error");
        }

    }
}
