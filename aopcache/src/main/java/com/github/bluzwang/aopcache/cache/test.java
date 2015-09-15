package com.github.bluzwang.aopcache.cache;

import java.lang.reflect.Method;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/15.
 */
public class test {
    public static String getMethodName(Class clz, Method method) {
        String clzName = clz.getName();
        String methodName = method.getName();
        return clzName + "." + methodName;
    }
    private void aaa() {

    }
    public static void main(String[] args) throws NoSuchMethodException {
        test t = new test();
        Class<? extends test> tClass = t.getClass();
        Method aaa = tClass.getDeclaredMethod("aaa");
        System.out.println(getMethodName(tClass, aaa));
    }
}
