package com.github.bluzwang.aopcache.cache;

import android.content.Context;
import io.paperdb.Paper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public class CacheUtil {
    private static Context sContext;
    private static Map<String, ICacheHolder> cacheHolders = new HashMap<String, ICacheHolder>();
    public static void addCacheHolder(String key, ICacheHolder cache) {
        cacheHolders.put(key, cache);
    }

    public static ICacheHolder getCacheHolder(String key) {
        return cacheHolders.get(key);
    }

    public static Context getApplicationContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        sContext = context.getApplicationContext();
        Paper.init(sContext);
    }
}
