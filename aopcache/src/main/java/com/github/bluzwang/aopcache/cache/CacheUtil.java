package com.github.bluzwang.aopcache.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public class CacheUtil {
    private static Map<String, ICacheHolder> cacheHolders = new HashMap<String, ICacheHolder>();
    public static void addCacheHolder(String key, ICacheHolder cache) {
        cacheHolders.put(key, cache);
    }

    static ICacheHolder getCacheHolder(String key) {
        return cacheHolders.get(key);
    }
}
