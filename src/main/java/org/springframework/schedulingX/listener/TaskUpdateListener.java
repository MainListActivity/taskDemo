package org.springframework.schedulingX.listener;


import org.springframework.schedulingX.annotation.Task;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-24.
 * 重要说明：
 * 修订历史：
 */
@FunctionalInterface
public interface TaskUpdateListener {
    void onUpdate(Task task);
}
