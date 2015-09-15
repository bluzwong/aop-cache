/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 *
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.cache;

import android.util.Log;
import io.paperdb.Paper;
import io.realm.Realm;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Aspect representing the cross cutting-concern: Method and Constructor Tracing.
 */
@Aspect
public class CacheAspect {

    private final static class Block {
        public Block(boolean started) {
            this.started = started;
        }

        public boolean started = false;
    }

    private static final Map<String, Block> blocks = new HashMap<String, Block>();

    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.cache.Cache * *(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithCache() {
    }


    public static void aopLog(String msg, long startTime) {
        if (CacheUtil.isNeedLog()) {
            if (startTime > 0) {
                long t = System.currentTimeMillis() - startTime;
                msg = "[" + t + "ms] " + msg;
                Log.i("aop-cache", msg);
            } else {
                Log.d("aop-cache", msg);
            }
        }
    }

    public static void aopLog(String msg) {
        aopLog(msg, -1);
    }

    public static void aopWarn(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.w("aop-cache", msg);
        }
    }

    @Around("methodAnnotatedWithCache()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        final long startTime = System.currentTimeMillis();
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class returnType = methodSignature.getReturnType();
        if (returnType != Observable.class) {
            aopWarn("!!! return type must be Observable<>, do not know about it? Google rxjava!");
            return joinPoint.proceed();
        }
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Cache cache = method.getAnnotation(Cache.class);
        final int level = cache.logLevel();
        final boolean needMemCache = cache.needMemCache();
        boolean tmpNeedDbCache = cache.needDbCache();
        if (!tmpNeedDbCache && !needMemCache) {
            aopWarn("@@@ don't need mem or database cache?");
            return joinPoint.proceed();
        }
        if (tmpNeedDbCache && CacheUtil.getApplicationContext() == null) {
            tmpNeedDbCache = false;
            aopWarn("!!!Cannot init database caused no Context. Please call CacheUtil.setApplicationContext(this); in onCreate().");
        }
        final boolean needDbCache = tmpNeedDbCache;
        final long memTimeOut = cache.memTimeOutMs();
        // final long memSecondTimeOut = cache.memSecondTimeOutMs();
        final long dbTimeOut = cache.dbTimeOutMs();

        final String className = methodSignature.getDeclaringType().getName();
        final String methodName = methodSignature.getName();
        StringBuilder buffer = new StringBuilder();
        buffer.append(className)
                .append(".")
                .append(methodName)
                .append(".");
        Object[] args = joinPoint.getArgs();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            boolean isIgnored = false;
            for (Annotation annotation : paramsAnnotations[i]) {
                if (annotation.annotationType() == Ignore.class) {
                    isIgnored = true;
                }
            }
            if (!isIgnored) {
                Object arg = args[i];
                buffer.append(arg.toString());
            }
            buffer.append("-");
        }
        final String key = buffer.toString();
        // final long dbSecondTimeOut = cache.dbSecondTimeOutMs();
        final ICacheHolder repo = DefaultCacheMemoryHolder.INSTANCE;
        final Object cachedValue = repo.get(key);
        if (cachedValue != null && needMemCache) {
            if (repo.getTimeOut(key) > System.currentTimeMillis() || repo.getTimeOut(key) <= 0) {
                aopLog(" hit in memory cache key:" + key + ((level > 0) ? "" : "  so return object:" + cachedValue), startTime);
                return Observable.just(cachedValue);
            } else {
                aopLog(" key:" + key + " in memory is out of time");
            }
        } else {
            aopLog(" key:" + key + " in memory is missed or out of time");
        }
        final Observable<Object> obResult = (Observable<Object>) joinPoint.proceed();
//    Log.d("bruce", "1 thread = " + Thread.currentThread().getName());
        return Observable.just(null)
                .map(new Func1<Object, Object>() {
                    @Override
                    public Object call(Object o) {
                        final long now = System.currentTimeMillis();
                        final Realm realm = Realm.getInstance(CacheUtil.getApplicationContext());
                        CacheInfo cacheInfo = realm.where(CacheInfo.class)
                                .equalTo("key", key)
                                .greaterThan("expTime", now)
                                .findFirst();
                        if (needDbCache && cacheInfo != null && Paper.exist(cacheInfo.getObjGuid())) {
                            Object objFromDb = Paper.get(cacheInfo.getObjGuid());
                            if (needMemCache) {
                                repo.put(key, objFromDb, cacheInfo.getExpTime(), 0);
                                aopLog(" hit in database cache key:" + key + ((level > 0) ? "" : "  so save to memory object:" + objFromDb));
                            }
                            aopLog(" hit in database cache key:" + key + ((level > 0) ? "" : "  so return object:" + objFromDb), startTime);
                            realm.close();
                            return objFromDb;
                        } else {
                            aopLog(" key:" + key + " in database is missed");
                        }
                        final Block block;
                        synchronized (this) {
                            if (!blocks.containsKey(key)) {
                                block = new Block(false);
                                blocks.put(key, block);
                            } else {
                                block = blocks.get(key);
                            }
                        }
                        final Object[] newResponse = new Object[1];
                        final CountDownLatch latch = new CountDownLatch(1);
                        synchronized (block) {
                            Object cachedValueAfterBlock = repo.get(key);
                            if (needMemCache && cachedValueAfterBlock != null) {
                                if (repo.getTimeOut(key) > now) {
                                    aopLog(" hit in blocked memory cache key:" + key + ((level > 0) ? "" : "  so return object:" + cachedValueAfterBlock), startTime);
                                    realm.close();
                                    return cachedValueAfterBlock;
                                }
                                //Log.d(cachedValueAfterBlock + "", " after newRequestStarted return cached key:" + key + " value" + cachedValueAfterBlock);
                            }
                            realm.refresh();
                            cacheInfo = realm.where(CacheInfo.class)
                                    .equalTo("key", key)
                                    .greaterThan("expTime", now)
                                    .findFirst();

                            if (needDbCache && cacheInfo != null && Paper.exist(cacheInfo.getObjGuid())) {
                                Object objFromDb = Paper.get(cacheInfo.getObjGuid());
                                if (needMemCache) {
                                    repo.put(key, objFromDb, cacheInfo.getExpTime(), 0);
                                    aopLog(" hit in database cache key:" + key + ((level > 0) ? "" : "  so save to memory object:" + objFromDb));
                                }
                                aopLog(" hit in blocked database cache key:" + key + ((level > 0) ? "" : "  so return object:" + cachedValueAfterBlock), startTime);
                                realm.close();
                                return objFromDb;
                            }

                            obResult.subscribe(new Action1<Object>() {
                                @Override
                                public void call(Object o) {
//                      Log.d("bruce", "2 thread = " + Thread.currentThread().getName());
                                    newResponse[0] = o;
                                    if (o != null && needMemCache) {
                                        repo.put(key, o, memTimeOut > 0 ? memTimeOut + System.currentTimeMillis() : Long.MAX_VALUE, 0);
                                        aopLog(" got new object save to memory cache key:" + key + ((level > 0) ? "" : "  object:" + o));
                                    }
                                    if (o != null && needDbCache) {

                                        //CacheObject cacheObject = new CacheObject();
                                        Realm realm = Realm.getInstance(CacheUtil.getApplicationContext());
                                        realm.beginTransaction();
                                        CacheInfo info;
                                        info = realm.where(CacheInfo.class).equalTo("key", key).findFirst();
                                        if (info == null) {
                                            info = realm.createObject(CacheInfo.class);
                                            info.setKey(key);
                                            String guid = UUID.randomUUID().toString();
                                            info.setObjGuid(guid);
                                        }
                                        info.setEditTime(now);
                                        info.setExpTime(dbTimeOut > 0 ? dbTimeOut + System.currentTimeMillis() : Long.MAX_VALUE);
                                        Paper.put(info.getObjGuid(), o);
                                        realm.commitTransaction();
                                        realm.close();
                                        aopLog(" got new object save to database cache key:" + key + ((level > 0) ? "" : "  object:" + o));
                                    }
                                    latch.countDown();
                                }
                            });
                        }
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                Log.d("bruce", "3 thread = " + Thread.currentThread().getName());
                        aopLog(" got new object return it: " + key + ((level > 0) ? "" : " object:" + newResponse[0]), startTime);
                        realm.close();
                        return newResponse[0];
                    }
                });
    }
}
