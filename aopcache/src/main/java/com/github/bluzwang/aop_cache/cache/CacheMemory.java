package com.github.bluzwang.aop_cache.cache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
@Retention(RetentionPolicy.CLASS) // 编译器
@Target({ ElementType.METHOD }) // 可注解在构造方法 方法上
public @interface CacheMemory {
     String holder() default "";
     long timeOutMs() default 0;
     long secondTimeOutMs() default 0;
}
