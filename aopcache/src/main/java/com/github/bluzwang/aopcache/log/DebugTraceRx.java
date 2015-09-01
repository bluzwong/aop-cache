/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 * @author Fernando Cejas (the android10 coder)
 */
package com.github.bluzwang.aopcache.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is being traced (debug mode only) and
 * will use {@link android.util.Log} to print debug data:
 * - Method name
 * - Total execution time
 * - Value (optional string parameter)
 *  debug trace for observable result
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
public @interface DebugTraceRx {
    int level() default 1;
    boolean enable() default true;
}
