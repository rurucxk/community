package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否需要登录来查看
 */

//声明作用域，可以放在方法上
@Target(ElementType.METHOD)
//声明什么时候能生效，运行时生效
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

}
