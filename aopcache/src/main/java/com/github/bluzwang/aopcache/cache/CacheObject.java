package com.github.bluzwang.aopcache.cache;


/**
 * Created by Bruce-Home on 2015/8/30.
 */
public class CacheObject {

    private Object object;

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    private long timeout;


    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
