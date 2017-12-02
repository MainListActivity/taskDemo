package com.example.demo.config;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.schedulingX.annotation.EnableProgressScheduling;
import org.springframework.schedulingX.annotation.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-3.
 * 重要说明：
 * 修订历史：
 */
@Configuration
@EnableProgressScheduling
public class ScheduleConfig implements SchedulingConfigurer, AsyncConfigurer {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();
    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    private TaskScheduler taskScheduler;

    private Map<CronTask, ScheduledFuture> cronTaskScheduledFutureMap;
    private List<Task> taskList;

    /**
     * 并行任务
     */
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskScheduler = new ThreadPoolTaskScheduler();//taskScheduler();
        cronTaskScheduledFutureMap = new HashMap<>();
        taskList = new ArrayList<>();
//
//        taskRegistrar.setTaskScheduler(concurrentTaskScheduler);
//        taskRegistrar
//                .getCronTaskList()
//                .forEach(
//                        cronTask -> {
//                            ScheduledFuture scheduledFuture = taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
//                            cronTaskScheduledFutureMap.put(cronTask, scheduledFuture);
//                            ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) cronTask.getRunnable();
//
//                            taskList.add(new Task(ATOMIC_INTEGER.getAndIncrement(), scheduledMethodRunnable.getMethod().toGenericString(), cronTask.getExpression(), Task.State.WRITTING_NEXT, cronTask.getTrigger().nextExecutionTime(new SimpleTriggerContext())));
//                        }
//                );
//        taskRegistrar.setCronTasksList(null);

    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void editTask(Task task) {
        if (taskScheduler == null || cronTaskScheduledFutureMap == null) return;
        cronTaskScheduledFutureMap.forEach((cronTask, scheduledFuture) -> {
            if (cronTask.getRunnable() instanceof ScheduledMethodRunnable) {
                ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) cronTask.getRunnable();
                log.info(scheduledMethodRunnable.getMethod().toGenericString());
                String methodName = scheduledMethodRunnable.getMethod().toGenericString();
                if (task.getTriggerName().equals(methodName) && scheduledFuture.cancel(true)) {
                    switch (task.getState()) {
                        case RUN:
                            cronTaskScheduledFutureMap.put(cronTask, taskScheduler.schedule(scheduledMethodRunnable, new CronTrigger(task.getCron())));
                            break;
                    }
                }
            }
        });

        System.out.println(task.getCron());
    }

    /**
     * 并行任务使用策略：多线程处理
     *
     * @return ThreadPoolTaskScheduler 线程池
     */
    @Bean(destroyMethod = "shutdown", name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20);
        scheduler.setThreadNamePrefix("task-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }


    /**
     * 异步任务
     */
    public Executor getAsyncExecutor() {
        return taskScheduler();
    }

    /**
     * 异步任务 异常处理
     */
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}

