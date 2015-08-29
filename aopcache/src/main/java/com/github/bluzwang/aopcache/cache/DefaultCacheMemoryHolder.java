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
    public final Map<String, Long> timeOutReturnInMap = new HashMap<String, Long>();

    @Override
    public void put(String key, Object value, long timeOutMs, long timeOutReturnInMs) {
        map.put(key, value);
        long now = System.currentTimeMillis();
        if (timeOutMs > 0) {
            timeOutMap.put(key, now + timeOutMs);
        }
        if (timeOutReturnInMs > 0) {
            timeOutReturnInMap.put(key, now + timeOutReturnInMs);
        }
    }

    public Object get(String key) {
        if (!map.containsKey(key)) {
            return null;
        }
        if (!timeOutMap.containsKey(key)) {
            return map.get(key);
        }
        long outTime = timeOutMap.get(key);
        long current = System.currentTimeMillis();
        long t = current - outTime;
        if (t <= 0) {
            return map.get(key);
        }
        /*map.remove(key);
        timeOutMap.remove(key);*/
        return null;
    }

    @Override
    public Object getBackUp(String key) {
        if (!timeOutReturnInMap.containsKey(key)) {
            return null;
        }
        long current = System.currentTimeMillis();
        long outTimeReturnIs = timeOutReturnInMap.get(key);
        long t = current - outTimeReturnIs;
        if (t <= 0 && map.containsKey(key)) {
            return map.get(key);
        }
        timeOutReturnInMap.remove(key);
        timeOutMap.remove(key);
        map.remove(key);
        return null;
    }
}
