package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * aop管理
 */
@Component
//用于标记一个类作为切面（Aspect）类
@Aspect
public class AlphaAspect {

    /**
      定义切点
     */
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){
    }

//    @Before("pointcut()")
//    public void before(){
//        System.out.println("before");
//    }
//
//    @After("pointcut()")
//    public void after(){
//        System.out.println("after");
//    }
//
//    @Around("pointcut()")
//    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
//        System.out.println("b");
//        Object proceed = joinPoint.proceed();
//        System.out.println("a");
//
//        return proceed;
//    }
}
