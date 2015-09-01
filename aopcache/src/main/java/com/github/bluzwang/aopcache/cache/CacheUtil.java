package com.github.bluzwang.aopcache.cache;

import android.content.Context;
import android.util.Log;
import io.paperdb.Paper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/8/27.
 */
public class CacheUtil {
    private static Context sContext;

    public static Context getApplicationContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        sContext = context.getApplicationContext();
        Paper.init(sContext);
    }

    private static boolean sNeedLog = false;

    public static boolean isNeedLog() {
        return sNeedLog;
    }

    public static void setNeedLog(boolean needLog) {
        sNeedLog = needLog;
    }

    public static void clearAllMemoryCache() {
        DefaultCacheMemoryHolder holder = DefaultCacheMemoryHolder.INSTANCE;
        int size =holder.map.size();
        holder.map.clear();
        holder.timeOutMap.clear();
        Log.d("aop-cache", "memory cache has been cleared size = " + size);
    }
    public static void clearAllDatabaseCache() {
        DefaultCacheMemoryHolder holder = DefaultCacheMemoryHolder.INSTANCE;
        int size =holder.map.size();
        holder.map.clear();
        holder.timeOutMap.clear();
        Log.d("aop-cache", "memory cache has been cleared size = " + size);
    }

    public static void removeMemoryCache(String classMethodString) {
        DefaultCacheMemoryHolder holder = DefaultCacheMemoryHolder.INSTANCE;
        List<Object> removeList = new ArrayList<>();
        for (String key : holder.map.keySet()) {
            // com.bruce.example.app.MainActivity.getResult
            if (key.startsWith(classMethodString)) {
                removeList.add(holder.get(key));
            }
        }
        for (Object o : removeList) {
            holder.map.remove(o);
        }
    }
}
