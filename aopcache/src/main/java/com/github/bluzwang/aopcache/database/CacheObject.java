package com.github.bluzwang.aopcache.database;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/31.
 */
public class CacheObject {


    private Object object;

    private long timeout;
    private long secondTimeout;





    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getSecondTimeout() {
        return secondTimeout;
    }

    public void setSecondTimeout(long secondTimeout) {
        this.secondTimeout = secondTimeout;
    }
}
