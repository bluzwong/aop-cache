/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 *
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.acache;

import android.util.Log;
import com.github.bluzwang.aopcache.cache.CacheUtil;
import com.github.bluzwang.aopcache.cache.DefaultCacheMemoryHolder;
import com.github.bluzwang.aopcache.cache.ICacheHolder;
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
public class CacheMemoryAspect {

    private final static class Block {
        public Block(boolean started) {
            this.started = started;
        }
        public boolean started = false;
    }

    private static final Map<String, Block> blocks = new HashMap<String, Block>();

    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.acache.CacheMemory * *(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithCacheMemory() {
    }

    @Around("methodAnnotatedWithCacheMemory()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.d("membruce", "inmem@@@@@@@@@@@@@@@@@@@@@@@@@");
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class returnType = methodSignature.getReturnType();
        if (returnType != Observable.class) {
            //Log.w("bruce", "return type is not Observable");
            Log.d("membruce", "inmem@@@@@@@@@@@@@@@@@@@@@@@@@ call point");
            Object proceed = joinPoint.proceed();
            return proceed;
        }
        final String className = methodSignature.getDeclaringType().getName();
        final String methodName = methodSignature.getName();
        StringBuilder buffer = new StringBuilder();
        buffer.append(className)
                .append("/")
                .append(methodName)
                .append("/");
        for (Object arg : joinPoint.getArgs()) {
            buffer.append(arg.toString()).append(":");
        }
        final String key = buffer.toString();
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        CacheMemory cache = method.getAnnotation(CacheMemory.class);
        final String repoKey = cache.holder();
        final long timeOut = cache.timeOutMs();
        final long timeOutReturnIn = cache.secondTimeOutMs();
        ICacheHolder tmpRepo = CacheUtil.getCacheHolder(repoKey);
        if (tmpRepo == null) {
            tmpRepo = DefaultCacheMemoryHolder.INSTANCE;
        }
        final Object cachedValue = tmpRepo.get(key);
        if (cachedValue != null) {
            Log.d(className, " return  cached key:" + key + " value" + cachedValue);
            return Observable.just(cachedValue);
        }

        Log.d("membruce", "inmem@@@@@@@@@@@@@@@@@@@@@@@@@ call point2");
        final Observable<Object> obResult = (Observable<Object>) joinPoint.proceed();
        final ICacheHolder repo = tmpRepo;
//    Log.d("bruce", "1 thread = " + Thread.currentThread().getName());

        return Observable.just(null)
                .map(new Func1<Object, Object>() {
                    @Override
                    public Object call(Object o) {
                        final Block block;
                        synchronized (this) {
                            if (!blocks.containsKey(key)) {
                                block = new Block(false);
                                blocks.put(key, block);
                            } else {
                                block = blocks.get(key);
                            }
                            if (block.started) {
                                Object backUp = repo.getBackUp(key);
                                if (backUp != null) {
                                    return backUp;
                                }
                            }
                        }
                        final Object[] newResponse = new Object[1];
                        final CountDownLatch latch = new CountDownLatch(1);
                        synchronized (block) {
                            block.started = true;
                            Object cachedValueAfterBlock = repo.get(key);
                            if (cachedValueAfterBlock != null) {
                                Log.d(cachedValueAfterBlock + "", " after newRequestStarted return cached key:" + key + " value" + cachedValueAfterBlock);
                                return cachedValueAfterBlock;
                            }
                            obResult.subscribe(new Action1<Object>() {
                                @Override
                                public void call(Object o) {
//                      Log.d("bruce", "2 thread = " + Thread.currentThread().getName());
                                    newResponse[0] = o;
                                    repo.put(key, o, timeOut, timeOutReturnIn);
                                    block.started = false;
                                    Log.d(o + "", "put ok ");
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
                        Log.d(newResponse[0] + "", " save  cached key:" + key + " value" + newResponse[0]);
                        return newResponse[0];
                    }
                });
    }
}