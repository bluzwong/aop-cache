package com.github.bluzwang.aopcache.cache;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Bruce-Home on 2015/8/30.
 */
public class CacheObject extends RealmObject{
    @PrimaryKey
    private String key;

    private String value;

    private long timeout;
    private long secondTimeout;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
