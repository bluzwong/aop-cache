package com.github.bluzwang.aopcache.cache;

import android.content.Context;
import android.util.Log;
import io.paperdb.Paper;
import io.realm.Realm;
import io.realm.RealmConfiguration;

import java.lang.reflect.Method;
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
//        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(sContext).build());
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
        //holder.timeOutMap.clear();
        Log.d("aop-cache", "memory cache has been cleared size = " + size);
    }
    public static void clearAllDatabaseCache() {
        //holder.timeOutMap.clear();
        Realm realm = Realm.getInstance(sContext);
        realm.clear(CacheInfo.class);
        realm.close();
        Log.d("aop-cache", "database cache has been cleared ");
    }

    public static void removeDataBaseCache(String classMethodString) {
        Realm realm = Realm.getInstance(sContext);
        CacheInfo info = realm.where(CacheInfo.class).equalTo("key", classMethodString).findFirst();
        if (info != null) {
            info.removeFromRealm();
        }
        realm.close();
    }

    public static void removeDataBaseCache(Class clz, Method method) {
        removeDataBaseCache(clz, method);
    }

    public static void removeMemoryCache(Class clz, Method method) {
        removeMemoryCache(getMethodName(clz, method));
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

    public static String getMethodName(Class clz, Method method) {
        String clzName = clz.getName();
        String methodName = method.getName();
        return clzName + "." + methodName;
    }
}
