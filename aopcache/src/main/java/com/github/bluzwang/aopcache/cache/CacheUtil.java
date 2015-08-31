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
}
