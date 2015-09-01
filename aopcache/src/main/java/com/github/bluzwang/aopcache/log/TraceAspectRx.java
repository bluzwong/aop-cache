/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 *
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.log;

import android.text.TextUtils;
import android.util.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Aspect representing the cross cutting-concern: Method and Constructor Tracing.
 */
@Aspect
public class TraceAspectRx {

    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.log.DebugTraceRx * *(..))";

    private static final String POINTCUT_CONSTRUCTOR =
            "execution(@com.github.bluzwang.aopcache.log.DebugTraceRx *.new(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithDebugTrace() {
    }

    @Pointcut(POINTCUT_CONSTRUCTOR)
    public void constructorAnnotatedDebugTrace() {
    }

    @Around("methodAnnotatedWithDebugTrace() || constructorAnnotatedDebugTrace()")
    public Object weaveJoinPoint(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Class returnType = methodSignature.getReturnType();
        final DebugTraceRx trace = methodSignature.getMethod().getAnnotation(DebugTraceRx.class);
        if (!trace.enable()) {
            return joinPoint.proceed();
        }
        final String className = methodSignature.getDeclaringType().getSimpleName();
        final String methodName = methodSignature.getName();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Observable<Object> obResult = (Observable<Object>) joinPoint.proceed();
        Observable<Object> logOb = Observable.just(null);
        return logOb.zipWith(obResult, new Func2<Object, Object, Object>() {
            @Override
            public Object call(Object nil, Object obj) {
                stopWatch.stop();
                String[] parameterNames = methodSignature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                String msg = TraceAspect.buildLogMessage(className, methodName, stopWatch.getTotalTimeMillis(), parameterNames, args, returnType, obj);
                switch (trace.level()) {
                    case 1:
                        Log.d(className, msg);
                        break;
                    case 2:
                        Log.i(className, msg);
                        break;
                    case 3:
                        Log.w(className, msg);
                        break;
                    case 4:
                        Log.e(className, msg);
                        break;
                    case 0:

                    default:
                        Log.v(className, msg);
                        break;
                }
                return obj;
            }
        });

    }
}
