package com.github.bluzwang.aopcache.cache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface Cache {
     boolean needMemCache() default true;
     long memTimeOutMs() default 0;

     //long memSecondTimeOutMs() default 0;
     boolean needDbCache() default true;
     long dbTimeOutMs() default 0;
     //long dbSecondTimeOutMs() default 0;

     /**
      * 0 full
      * 1 simple info
      * @return
      */
     int logLevel() default 0;
}
