package com.github.bluzwang.aopcache.cache;

import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public enum DefaultCacheMemoryHolder implements ICacheHolder {
    INSTANCE;
    public final Map<String, ObjectAndTimeOut> map = new LruMap<String, ObjectAndTimeOut>(100, 0.75f, true);
//    public final Map<String, Long> timeOutMap = new HashMap<String, Long>();

    @Override
    public void put(String key, Object value, long timeOutMs, long timeOutReturnInMs) {
        map.put(key, new ObjectAndTimeOut(value, timeOutMs));
    }

    public Object get(String key) {
        if (!map.containsKey(key)) {
            return null;
        }
        return map.get(key).obj;
    }
    public long getTimeOut(String key) {
        if (!map.containsKey(key)) {
            return 0;
        }
        return map.get(key).timeOut;
    }
}
