package com.github.bluzwang.aop_cache.cache;

import android.util.Log;

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
        // 有缓存
        if (!timeOutMap.containsKey(key)) {
            // 没有设定缓存超时 直接返回数据
            Log.d(key, "没有设定缓存超时 直接返回");
            return map.get(key);
        }
        // 有设定缓存超时
        long outTime = timeOutMap.get(key);
        long current = System.currentTimeMillis();
        long t = current - outTime;
        if (t <= 0) {
            // 没有超时 直接返回数据
            Log.d(key, "没有超时 直接返回数据");
            return map.get(key);
        }
        // 全部超时了并且不存在 删除相关数据 最后返回null
        /*map.remove(key);
        timeOutMap.remove(key);*/
        Log.d(key, "main超时了 " + t + "ms 返回null");
        return null;
    }

    @Override
    public Object getBackUp(String key) {
        if (!timeOutReturnInMap.containsKey(key)) {
            // 不存在备用返回
            return null;
        }
        // 存在备用返回
        long current = System.currentTimeMillis();
        long outTimeReturnIs = timeOutReturnInMap.get(key);
        long t = current - outTimeReturnIs;
        if (t <= 0 && map.containsKey(key)) {
            // 备用返回还有效
            Log.d(key, "备用还有 " + -t + "ms 超时. 返回备用数据");
            return map.get(key);
        }
        // 备用返回都无效了
        Log.d(key, "备用超时了 " + t + "ms 删除相关数据 最后返回null");
        timeOutReturnInMap.remove(key);
        timeOutMap.remove(key);
        map.remove(key);
        return null;
    }
}
