package com.github.bluzwang.aopcache.acache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface CacheMemory {
     String holder() default "";
     long timeOutMs() default 0;
     long secondTimeOutMs() default 0;
}
