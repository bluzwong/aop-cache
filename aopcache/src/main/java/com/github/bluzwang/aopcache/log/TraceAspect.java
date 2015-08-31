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

/**
 * Aspect representing the cross cutting-concern: Method and Constructor Tracing.
 */
@Aspect
public class TraceAspect {

    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.log.DebugTrace * *(..))";

    private static final String POINTCUT_CONSTRUCTOR =
            "execution(@com.github.bluzwang.aopcache.log.DebugTrace *.new(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithDebugTrace() {
    }

    @Pointcut(POINTCUT_CONSTRUCTOR)
    public void constructorAnnotatedDebugTrace() {
    }

    @Around("methodAnnotatedWithDebugTrace() || constructorAnnotatedDebugTrace()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.d("debugbruce", "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% in debug ");
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        DebugTrace trace = methodSignature.getMethod().getAnnotation(DebugTrace.class);
        if (!trace.enable()) {
            return joinPoint.proceed();
        }
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = joinPoint.proceed();
        stopWatch.stop();

        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Class returnType = methodSignature.getReturnType();
        String msg = buildLogMessage(className, methodName, stopWatch.getTotalTimeMillis(), parameterNames, args, returnType, result);
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
        return result;
    }

    /**
     * Create a log message.
     *
     * @param methodName     A string with the method name.
     * @param methodDuration Duration of the method in milliseconds.
     * @return A string representing message.
     */
    private static String buildLogMessage(String className, String methodName, long methodDuration, String[] parameterNames, Object[] args, Class returnType, Object returnValue) {
        StringBuilder message = new StringBuilder();
        message.append("\nLog\n")
                .append(TextUtils.isEmpty(className) ? "NO CLASS NAME!" : className)
                .append("\n -->")
                .append(methodName)
                .append(" (");
        for (int i = 0; i < parameterNames.length; i++) {
            message.append(parameterNames[i]).append("=").append("\"").append(args[i]).append("\"");
            if (i != parameterNames.length -1) {
                message.append(", ");
            }
        }
        message.append(")")
                .append("\n <--")
                .append(methodName)
                .append(" [")
                .append(methodDuration)
                .append("ms")
                .append("] = ")
                .append("\"")
                .append(returnValue).append("\"");
        return message.toString();
    }
}
