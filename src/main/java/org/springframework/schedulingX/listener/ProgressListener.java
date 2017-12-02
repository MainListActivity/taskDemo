package org.springframework.schedulingX.listener;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
@FunctionalInterface
public interface ProgressListener {
    void progress(int p);
}
