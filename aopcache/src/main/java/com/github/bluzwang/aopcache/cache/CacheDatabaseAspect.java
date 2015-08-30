package com.github.bluzwang.aopcache.cache;


import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import io.realm.Realm;
import io.realm.RealmQuery;
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
public class CacheDatabaseAspect {

    private final static class Block {
        public Block(boolean started) {
            this.started = started;
        }
        public boolean started = false;
    }

    private static final Map<String, Block> blocks = new HashMap<String, Block>();

    // 要切入的方法名
    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.cache.CacheDatabase * *(..))";

    // 切入该方法
    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithCacheDatabase() {
    }

    // 在切入方法的周围
    @Around("methodAnnotatedWithCacheDatabase()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        final Context context = CacheUtil.getApplicationContext();
        if (context == null) {
            Log.d("bruce", " context is not settled ");
            return joinPoint.proceed();
        }
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();


        final CacheDatabase cache = method.getAnnotation(CacheDatabase.class);
        final long timeout = cache.timeOutMs();
        final long secondTimeout = cache.secondTimeOutMs();
        final Class objectClass = cache.gsonClass();

        Class returnType = methodSignature.getReturnType();
        if (returnType != Observable.class) {
            Log.w("bruce", "return gsonClass is not Observable");
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

        final Observable<Object> obResult = (Observable<Object>) joinPoint.proceed();
//    Log.d("bruce", "1 thread = " + Thread.currentThread().getName());

        return Observable.just(null)
                .map(new Func1<Object, Object>() {
                    @Override
                    public Object call(Object o) {
                        Realm realm = Realm.getInstance(context);
                        RealmQuery<CacheObject> query = realm.where(CacheObject.class);
                        long now = System.currentTimeMillis();
                        Log.d("bruce", now + " is now");
                        query.equalTo("key", key);
                        if (timeout != 0) {
                            query.greaterThan("timeout", now);
                        }
                        CacheObject cacheObject = query.findFirst();
                        if (cacheObject != null) {
                            if (objectClass == String.class) {
                                //Log.d("bruce", " type is not defined ");
                                String value = cacheObject.getValue();
                                long sub = cacheObject.getTimeout() - now;
                                Log.d(className, " data base return  cached key:" + key + " value" +  value+ " out time = " + cacheObject.getTimeout() + " sub = " + sub);
                                realm.close();
                                return value;
                            }
                            Object fromJson = new Gson().fromJson(cacheObject.getValue(), objectClass);
                            long sub = cacheObject.getTimeout() - now;
                            Log.d(className, " data base return  cached key:" + key + " value" + fromJson + " out time = " + cacheObject.getTimeout() + " sub = " + sub);
                            realm.close();
                            return fromJson;
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
                        if (block.started) {
                            Log.d(key, "开始请求了 尝试返回备用");
                            RealmQuery<CacheObject> query2 = realm.where(CacheObject.class);
                            query2.equalTo("key", key);
                            if (secondTimeout != 0) {
                                query2.greaterThan("secondTimeout", now);
                            }
                            CacheObject secondCacheObject = query2.findFirst();
                            if (secondCacheObject != null) {
                                if (objectClass == String.class) {
                                    //Log.d("bruce", " type is not defined ");
                                    String value = secondCacheObject.getValue();
                                    realm.close();
                                    return value;
                                }
                                Object fromJson = new Gson().fromJson(secondCacheObject.getValue(), objectClass);
                                realm.close();
                                return fromJson;
                            }
                        } else {
                            Log.d(key, "还没开始请求进入 block");
                        }
                        final Object[] newResponse = new Object[1];
                        final CountDownLatch latch = new CountDownLatch(1);
                        synchronized (block) {
                            Log.d(key, "已进入block");
                            block.started = true;
                            RealmQuery<CacheObject> query3 = realm.where(CacheObject.class);
                            long now2 = System.currentTimeMillis();
                            query3.equalTo("key", key);
                            if (timeout != 0) {
                                query3.greaterThan("timeout", now2);
                            }
                            CacheObject cacheObject2 = query3.findFirst();
                            if (cacheObject2 != null) {
                                if (objectClass == String.class) {
                                    //Log.d("bruce", " type is not defined ");
                                    String value = cacheObject2.getValue();
                                    Log.d(className, "data base after newRequestStarted return cached key:" + key + " value" + value);
                                    realm.close();
                                    return value;
                                }
                                Object fromJson = new Gson().fromJson(cacheObject2.getValue(), objectClass);
                                Log.d(fromJson + "", " data base after newRequestStarted return cached key:" + key + " value" + fromJson);
                                realm.close();
                                return fromJson;
                            }
                            obResult.subscribe(new Action1<Object>() {
                                @Override
                                public void call(Object o) {
//                      Log.d("bruce", "2 thread = " + Thread.currentThread().getName());
                                    newResponse[0] = o;
                                    long now = System.currentTimeMillis();

                                    Realm realm = Realm.getInstance(context);
                                    realm.beginTransaction();
                                    CacheObject responseObject = realm.where(CacheObject.class).equalTo("key", key).findFirst();
                                    if (responseObject == null) {
                                        responseObject = realm.createObject(CacheObject.class);
                                        responseObject.setKey(key);
                                    }
                                    responseObject.setTimeout(timeout == 0? Long.MAX_VALUE: now + timeout);
                                    responseObject.setSecondTimeout(secondTimeout == 0? Long.MAX_VALUE: now + secondTimeout);
                                    String valueToSave;
                                    if (objectClass == String.class) {
                                        valueToSave = o.toString();
                                    } else {
                                        valueToSave = new Gson().toJson(o);
                                    }
                                    responseObject.setValue(valueToSave);

                                    realm.commitTransaction();
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
                        Log.d(key, "已退出block");
//                Log.d("bruce", "3 thread = " + Thread.currentThread().getName());
                        Log.d(newResponse[0] + "", " data base save  cached key:" + key + " value" + newResponse[0]);
                        realm.close();
                        return newResponse[0];
                    }
                });
    }
}
