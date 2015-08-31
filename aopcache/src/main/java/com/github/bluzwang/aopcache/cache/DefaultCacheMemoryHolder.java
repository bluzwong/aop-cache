package com.github.bluzwang.aopcache.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public enum DefaultCacheMemoryHolder implements ICacheHolder {
    INSTANCE;
    public final Map<String, Object> map = new HashMap<String, Object>();
    public final Map<String, Long> timeOutMap = new HashMap<String, Long>();

    @Override
    public void put(String key, Object value, long timeOutMs, long timeOutReturnInMs) {
        map.put(key, value);
        timeOutMap.put(key, timeOutMs);
    }

    public Object get(String key) {
        if (!map.containsKey(key)) {
            return null;
        }
        return map.get(key);
    }
    public long getTimeOut(String key) {
        if (!timeOutMap.containsKey(key)) {
            return 0;
        }
        return timeOutMap.get(key);
    }
}
