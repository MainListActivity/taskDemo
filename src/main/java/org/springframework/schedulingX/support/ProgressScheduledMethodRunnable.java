package org.springframework.schedulingX.support;

import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.schedulingX.listener.ProgressListener;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
public class ProgressScheduledMethodRunnable extends ScheduledMethodRunnable {

    private final ProgressListener progressListener;

    public ProgressScheduledMethodRunnable(Object target, Method method, ProgressListener progressListener) {
        super(target, method);
        this.progressListener = progressListener;
    }

    @Override
    public void run() {
        try {
            ReflectionUtils.makeAccessible(super.getMethod());
            super.getMethod().invoke(super.getTarget(), progressListener);
        } catch (InvocationTargetException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
        } catch (IllegalAccessException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

}

