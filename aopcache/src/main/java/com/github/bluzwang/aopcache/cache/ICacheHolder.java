package com.github.bluzwang.aopcache.cache;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public interface ICacheHolder {
    void put(String key, Object value, long timeOutMs, long timeOutReturnInMs);
    Object get(String key);
     long getTimeOut(String key);
}
