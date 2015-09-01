/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 *
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.validation;

import android.util.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Aspect representing the cross cutting-concern: Method and Constructor Tracing.
 */
@Aspect
public class TellMeAspect {

    private static final String POINTCUT_METHOD =
            "execution(@com.github.bluzwang.aopcache.validation.TellMe * *(..))";

    private static final String POINTCUT_CONSTRUCTOR =
            "execution(@com.github.bluzwang.aopcache.validation.TellMe *.new(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithDebugTrace() {
    }

    @Pointcut(POINTCUT_CONSTRUCTOR)
    public void constructorAnnotatedDebugTrace() {
    }

    @Around("methodAnnotatedWithDebugTrace() || constructorAnnotatedDebugTrace()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Method method = methodSignature.getMethod();
        List<String> validate = validateFiled(method.getAnnotation(TellMeIfError.class), args, methodSignature.getParameterNames());
        if (validate.size() > 0) {
            //Log.d("tellme", "ok");
            StringBuffer buffer = new StringBuffer();
            buffer.append("error !!! -----> \n");
            for (String s : validate) {
                buffer.append(s);
                buffer.append("\n");
            }
            Log.e("tellme", buffer.toString());
        }
        Object result = joinPoint.proceed();
        return result;
    }

    public List<String> validateFiled(TellMeIfError valiedatefield, Object[] args, String[] names)
            throws Exception {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < valiedatefield.whenParamIndex().length; i++) {
            Object arg = null;
            String fieldName;
                arg = args[valiedatefield.whenParamIndex()[i]];
                fieldName = names[valiedatefield.whenParamIndex()[i]];

            if (valiedatefield.notNull().length > i && valiedatefield.notNull()[i]) {
                if (arg == null)
                    errors.add( fieldName + " is null");
            } else {
                if (arg == null)
                    continue;
//                    return null
            }
            if (valiedatefield.maxVal().length > i && valiedatefield.maxVal()[i] != -1) {
                if ((Integer) arg > valiedatefield.maxVal()[i])
                    errors.add(fieldName + " is: " + arg + " bigger than maxVal: " + valiedatefield.maxVal()[i]);
            }
            if (valiedatefield.minVal().length > i && valiedatefield.minVal()[i] != -1) {
                if ((Integer) arg < valiedatefield.minVal()[i])
                    errors.add( fieldName + " is: " + arg + " less than minVal: " + valiedatefield.minVal()[i]);
            }
            if (valiedatefield.regStr().length > i && !"".equals(valiedatefield.regStr()[i])) {
                if (arg instanceof String) {
                    if (!((String) arg).matches(valiedatefield.regStr()[i]))
                        errors.add( fieldName + " is: " + arg + " not match reg: " + valiedatefield.regStr()[i]);
                } else {
                    errors.add( fieldName + " is: " + arg + "not match reg: " + valiedatefield.regStr()[i]);
                }
            }
        }
        return errors;
    }
}
