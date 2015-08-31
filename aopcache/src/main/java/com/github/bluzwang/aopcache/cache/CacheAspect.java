/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 *
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.cache;

import android.util.Log;
import io.paperdb.Paper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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


    private void aopLog(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.d("aop-cache", msg);
        }
    }
    private void aopWarn(String msg) {
        if (CacheUtil.isNeedLog()) {
            Log.w("aop-cache", msg);
        }
    }
    @Around("methodAnnotatedWithCache()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class returnType = methodSignature.getReturnType();
        if (returnType != Observable.class) {
            aopWarn("!!! return type must be Observable<>, do not know about it? Google rxjava!");
            return joinPoint.proceed();
        }
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Cache cache = method.getAnnotation(Cache.class);
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
        for (Object arg : joinPoint.getArgs()) {
            buffer.append(arg.toString()).append("-");
        }
        final String key = buffer.toString();
        // final long dbSecondTimeOut = cache.dbSecondTimeOutMs();
        final ICacheHolder repo = DefaultCacheMemoryHolder.INSTANCE;
        final Object cachedValue = repo.get(key);
        if (cachedValue != null && needMemCache) {
            if (repo.getTimeOut(key) > System.currentTimeMillis() || repo.getTimeOut(key) <= 0) {
                aopLog(" hit in memory cache key:" + key + "  so return object:" + cachedValue);
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
                        long now = System.currentTimeMillis();
                        if (needDbCache && Paper.exist(key)) {
                            CacheObject cacheObject = Paper.get(key);
                            if (cacheObject.getTimeout() > now || cacheObject.getTimeout() <= 0) {
                                Object objFromDb = cacheObject.getObject();
                                if (needMemCache) {
                                    repo.put(key, objFromDb, cacheObject.getTimeout(), 0);
                                    aopLog(" hit in database cache key:" + key + "  so save to memory object:" + cacheObject.getObject());
                                }
                                aopLog(" hit in database cache key:" + key + "  so return object:" + cacheObject.getObject());
                                return objFromDb;
                            } else {
                                aopLog(" key:" + key + " in database is out of time");
                            }
                        } else if(!Paper.exist(key)) {
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
                                    aopLog(" hit in blocked memory cache key:" + key + "  so return object:" + cachedValueAfterBlock);
                                    return cachedValueAfterBlock;
                                }
                                //Log.d(cachedValueAfterBlock + "", " after newRequestStarted return cached key:" + key + " value" + cachedValueAfterBlock);
                            }
                            if (needDbCache && Paper.exist(key)) {
                                CacheObject cacheObject = Paper.get(key);
                                if (cacheObject.getTimeout() > now) {
                                    aopLog(" hit in blocked database cache key:" + key + "  so return object:" + cachedValueAfterBlock);
                                    return cacheObject.getObject();
                                }
                            }

                            obResult.subscribe(new Action1<Object>() {
                                @Override
                                public void call(Object o) {
//                      Log.d("bruce", "2 thread = " + Thread.currentThread().getName());
                                    newResponse[0] = o;
                                    if (needMemCache) {
                                        repo.put(key, o, memTimeOut  > 0 ?memTimeOut + System.currentTimeMillis(): Long.MAX_VALUE, 0);
                                        aopLog(" got new object save to memory cache key:" + key + "  object:" + o);
                                    }
                                    if (needDbCache) {
                                        CacheObject cacheObject = new CacheObject();
                                        cacheObject.setObject(o);
                                        cacheObject.setTimeout(dbTimeOut > 0 ?dbTimeOut + System.currentTimeMillis(): Long.MAX_VALUE);
                                        Paper.put(key, cacheObject);
                                        aopLog(" got new object save to database cache key:" + key + "  object:" + o);
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
                        aopLog(" got new object return it: " + key + " object:" + newResponse[0]);
                        return newResponse[0];
                    }
                });
    }
}
