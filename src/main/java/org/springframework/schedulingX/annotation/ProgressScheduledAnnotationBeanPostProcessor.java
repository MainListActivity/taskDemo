package org.springframework.schedulingX.annotation;

import org.springframework.schedulingX.listener.ProgressListener;
import org.springframework.schedulingX.listener.TaskUpdateListener;
import org.springframework.schedulingX.support.ProgressScheduledMethodRunnable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by y
 * on 2017/11/27
 */
public class ProgressScheduledAnnotationBeanPostProcessor extends ScheduledAnnotationBeanPostProcessor {
    /**
     * The default name of the {@link TaskScheduler} bean to pick up: "taskScheduler".
     * <p>Note that the initial lookup happens by type; this is just the fallback
     * in case of multiple scheduler beans found in the context.
     */
    private static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();


    private final Log logger = LogFactory.getLog(getClass());

    private Object scheduler;

    private StringValueResolver embeddedValueResolver;

    private String beanName;

    private BeanFactory beanFactory;

    private ApplicationContext applicationContext;

    private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

    private final Set<Class<?>> nonAnnotatedClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));

    private final List<TaskUpdateListener> taskUpdateListeners = new ArrayList<>(8);

//    private final Map<Object, Set<ScheduledTask>> scheduledTasks =
//            new IdentityHashMap<>(16);

    private final List<Task> taskList = new ArrayList<>(16);


    @Override
    public int getOrder() {
        super.getOrder();
        return LOWEST_PRECEDENCE;
    }

    /**
     * Set the {@link org.springframework.scheduling.TaskScheduler} that will invoke
     * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
     * to be wrapped as a TaskScheduler.
     * <p>If not specified, default scheduler resolution will apply: searching for a
     * unique {@link TaskScheduler} bean in the context, or for a {@link TaskScheduler}
     * bean named "taskScheduler" otherwise; the same lookup will also be performed for
     * a {@link ScheduledExecutorService} bean. If neither of the two is resolvable,
     * a local single-threaded default scheduler will be created within the registrar.
     *
     * @see #DEFAULT_TASK_SCHEDULER_BEAN_NAME
     */
    public void setScheduler(Object scheduler) {
        this.scheduler = scheduler;
        super.setScheduler(scheduler);
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
        super.setEmbeddedValueResolver(resolver);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
        super.setBeanName(beanName);
    }

    /**
     * Making a {@link BeanFactory} available is optional; if not set,
     * {@link SchedulingConfigurer} beans won't get autodetected and
     * a {@link #setScheduler scheduler} has to be explicitly configured.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
    }

    /**
     * Setting an {@link ApplicationContext} is optional: If set, registered
     * tasks will be activated in the {@link ContextRefreshedEvent} phase;
     * if not set, it will happen at {@link #afterSingletonsInstantiated} time.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        if (this.beanFactory == null) {
            this.beanFactory = applicationContext;
        }
        super.setApplicationContext(applicationContext);
    }


    @Override
    public void afterSingletonsInstantiated() {
        // Remove resolved singleton classes from cache
        this.nonAnnotatedClasses.clear();

        if (this.applicationContext == null) {
            // Not running in an ApplicationContext -> register tasks early...
            finishRegistration();
        }
        super.afterSingletonsInstantiated();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.applicationContext) {
            // Running in an ApplicationContext -> register tasks this late...
            // giving other ContextRefreshedEvent listeners a chance to perform
            // their work at the same time (e.g. Spring Batch's job registration).
            finishRegistration();
        }
        super.onApplicationEvent(event);
    }

    private void finishRegistration() {
        if (this.scheduler != null) {
            this.registrar.setScheduler(this.scheduler);
        }

        if (this.beanFactory instanceof ListableBeanFactory) {
            Map<String, SchedulingConfigurer> configurers =
                    ((ListableBeanFactory) this.beanFactory).getBeansOfType(SchedulingConfigurer.class);
            for (SchedulingConfigurer configurer : configurers.values()) {
                configurer.configureTasks(this.registrar);
            }
            Map<String, TaskUpdateListener> listenerMap = ((ListableBeanFactory) this.beanFactory).getBeansOfType(TaskUpdateListener.class);
            taskUpdateListeners.addAll(listenerMap.values());
        }

        if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to find scheduler by type");
            try {
                // Search for TaskScheduler bean...
                this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, false));
            } catch (NoUniqueBeanDefinitionException ex) {
                logger.debug("Could not find unique TaskScheduler bean", ex);
                try {
                    this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, true));
                } catch (NoSuchBeanDefinitionException ex2) {
                    if (logger.isInfoEnabled()) {
                        logger.info("More than one TaskScheduler bean exists within the context, and " +
                                "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                                "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                                "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                                ex.getBeanNamesFound());
                    }
                }
            } catch (NoSuchBeanDefinitionException ex) {
                logger.debug("Could not find default TaskScheduler bean", ex);
                // Search for ScheduledExecutorService bean next...
                try {
                    this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, false));
                } catch (NoUniqueBeanDefinitionException ex2) {
                    logger.debug("Could not find unique ScheduledExecutorService bean", ex2);
                    try {
                        this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, true));
                    } catch (NoSuchBeanDefinitionException ex3) {
                        if (logger.isInfoEnabled()) {
                            logger.info("More than one ScheduledExecutorService bean exists within the context, and " +
                                    "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                                    "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                                    "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                                    ex2.getBeanNamesFound());
                        }
                    }
                } catch (NoSuchBeanDefinitionException ex2) {
                    logger.debug("Could not find default ScheduledExecutorService bean", ex2);
                    // Giving up -> falling back to default scheduler within the registrar...
                    logger.info("No TaskScheduler/ScheduledExecutorService bean found for scheduled processing");
                }
            }
        }

        this.registrar.afterPropertiesSet();
    }

    private <T> T resolveSchedulerBean(Class<T> schedulerType, boolean byName) {
        if (byName) {
            T scheduler = this.beanFactory.getBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        DEFAULT_TASK_SCHEDULER_BEAN_NAME, this.beanName);
            }
            return scheduler;
        } else if (this.beanFactory instanceof AutowireCapableBeanFactory) {
            NamedBeanHolder<T> holder = ((AutowireCapableBeanFactory) this.beanFactory).resolveNamedBean(schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        holder.getBeanName(), this.beanName);
            }
            return holder.getBeanInstance();
        } else {
            return this.beanFactory.getBean(schedulerType);
        }
    }


    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, String beanName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (!this.nonAnnotatedClasses.contains(targetClass)) {
            Map<Method, Set<ProgressScheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<Set<ProgressScheduled>>) method -> {
                        Set<ProgressScheduled> scheduledMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                                method, ProgressScheduled.class, ProgressSchedules.class);
                        return (!scheduledMethods.isEmpty() ? scheduledMethods : null);
                    });
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.add(targetClass);
                if (logger.isTraceEnabled()) {
                    logger.trace("No @ProgressScheduled annotations found on bean class: " + bean.getClass());
                }
            } else {
                // Non-empty set of methods
                for (Map.Entry<Method, Set<ProgressScheduled>> entry : annotatedMethods.entrySet()) {
                    Method method = entry.getKey();
                    for (ProgressScheduled scheduled : entry.getValue()) {
                        Task task = new Task();
                        task.copyValueFromProgressScheduled(scheduled);
                        task.addScheduledInfo(null, bean, method);
                        processScheduled(task, method, bean);
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(annotatedMethods.size() + " @ProgressScheduled methods processed on bean '" + beanName +
                            "': " + annotatedMethods);
                }
            }
        }
        super.postProcessAfterInitialization(bean, beanName);
        return bean;
    }

    public List<Task> getTasks() {
        return taskList;
    }

    /**
     * 根据task对象进行调度
     *
     * @param task 修改的task对象
     */
    public void setTask(Task task) {

        if (taskList.stream()
                .filter(t -> t.getId() == task.getId() && t.getTriggerName().equals(task.getTriggerName()))
                .count() <= 0) {
            //在当前维护的taskList中没有找到传入的这个id——根据传入的TriggerName创建一个新的任务，并调度起来
            List<Task> rtn = taskList.stream()
                    .filter(t -> t.getTriggerName().equals(task.getTriggerName()))
                    .limit(1).collect(Collectors.toList());
            //.forEach(t -> this.processScheduled(task, t.getMethod(), t.getBean()));
            if (rtn != null) {
                rtn.forEach(t -> this.processScheduled(task, t.getMethod(), t.getBean()));
            }
        } else {
            taskList.stream()//拿到我们维护的taskList的流
                    //添加过滤，只关注后台维护的list中和传入task对象id相等并且触发器名相同的对象
                    .filter(t -> t.getId() == task.getId() && t.getTriggerName().equals(task.getTriggerName()))
                    //得到的结果数最多为1,我们只关注它有没有结果，以及其中的一个结果，目的就是拿到这个任务的method对象
                    .limit(1)
                    .forEach(t -> {
                        //从传入的task中获取数据，并保存到list中匹配的task对象，这样做的好处就是不用频繁操作列表
                        t.copyValueFromTask(task);
                        //拿到曾经调度过这个任务的Future对象
                        ScheduledTask scheduledTask = t.getScheduledTask();
                        Object bean = t.getBean();
                        Method method = t.getMethod();
                        if (scheduledTask != null && bean != null && method != null) {
                            //取消之前调度的任务
                            scheduledTask.cancel();
                            //当传入task对象的状态不是stop时开启下一次调度，反之则说明操作者想要停止当前任务
                            if (!t.getState().equals(Task.State.STOP))
                                this.processScheduled(t, method, bean);
                        }
                        notifyTaskListener(t);
                    });
        }
    }

    private void notifyTaskListener(Task task) {
        taskUpdateListeners.forEach((listener) -> listener.onUpdate(task));
    }

    /**
     * 通知外部哪个任务发生了变化，进度当前是多少，下次实行时间是多少
     *
     * @param task              who
     * @param progress          what
     * @param nextExecutionTime what
     * @param isRate            通过这个标识获取下次执行的时间
     */
    private void setTask(Task task, int progress, Date nextExecutionTime, boolean isRate) {
        task.setProgress(progress);
        if (progress == 0) {
            task.setState(Task.State.RUN);
            if (isRate)
                task.setNextExecute(nextExecutionTime);
            else
                task.setNextExecute(null);
        } else if (progress == 100) {
            task.setState(Task.State.WAITING_NEXT);
            if (!isRate)
                task.setNextExecute(nextExecutionTime);
            task.setLastExecute(new Date());
        }
        notifyTaskListener(task);
    }


    private void processScheduled(Task scheduled, Method method, Object bean) {
        try {
            Method invokableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
            Runnable runnable;
            Assert.isTrue(invokableMethod.getParameterTypes().length == 1 && invokableMethod.getParameterTypes()[0].equals(ProgressListener.class), "Only one-arg typeof ProgressListener methods may be annotated with @ProgressScheduled");
            boolean processedSchedule = false;
            boolean isScheduled = scheduled.getScheduledTask() != null;
            String errorMessage =
                    "Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

//            Set<ScheduledTask> tasks = new LinkedHashSet<>(4);

            // Determine initial delay
            long initialDelay = scheduled.getInitialDelay();
            String initialDelayString = scheduled.getInitialDelayString();
            if (StringUtils.hasText(initialDelayString)) {
                Assert.isTrue(initialDelay < 0, "Specify 'initialDelay' or 'initialDelayString', not both");
                if (this.embeddedValueResolver != null) {
                    initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
                }
                try {
                    initialDelay = Long.parseLong(initialDelayString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into integer");
                }
            }

            // Check cron expression
            String cron = scheduled.getCron();
            if (StringUtils.hasText(cron)) {
                Assert.isTrue(initialDelay == -1, "'initialDelay' not supported for cron triggers");
                processedSchedule = true;
                String zone = scheduled.getZone();
                if (this.embeddedValueResolver != null) {
                    cron = this.embeddedValueResolver.resolveStringValue(cron);
                    zone = this.embeddedValueResolver.resolveStringValue(zone);
                }
                TimeZone timeZone;
                if (StringUtils.hasText(zone)) {
                    timeZone = StringUtils.parseTimeZoneString(zone);
                } else {
                    timeZone = TimeZone.getDefault();
                }
                Task task;
                if (isScheduled) {
                    task = scheduled;
                } else {
                    task = new Task(ATOMIC_INTEGER.getAndIncrement(),
                            method.toGenericString(),
                            cron,
                            Task.State.WAITING_NEXT,
                            null,
                            scheduled.getTaskName());
                }
                runnable = new ProgressScheduledMethodRunnable(bean,
                        invokableMethod,
                        p -> this.taskList.stream().
                                filter(t -> task.getId() == t.getId())
                                .forEach(t -> setTask(t, p, new CronTrigger(t.getCron()).nextExecutionTime(new SimpleTriggerContext()), false))
                );
                CronTask cronTask = new CronTask(runnable, new CronTrigger(cron, timeZone));
                ScheduledTask scheduledTask = this.registrar.scheduleCronTask(cronTask);
                task.addScheduledInfo(scheduledTask, bean, method);
                if (isScheduled)
                    scheduled.addScheduledInfo(scheduledTask, bean, method);
                else {
                    this.taskList.add(task);
                }
//                tasks.add(scheduledTask);
            }

            // At this point we don't need to differentiate between initial delay set or not anymore
            if (initialDelay < 0) {
                initialDelay = 0;
            }

            // Check fixed delay
            long fixedDelay = scheduled.getFixedDelay();
            if (fixedDelay >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                final long fixedDelayTmp = fixedDelay;
                Task task;
                if (isScheduled) task = scheduled;
                else
                    task = new Task(ATOMIC_INTEGER.getAndIncrement(),
                            method.toGenericString(),
                            fixedDelayTmp,
                            -1,
                            Task.State.WAITING_NEXT,
                            null,
                            scheduled.getTaskName());
                runnable = new ProgressScheduledMethodRunnable(bean,
                        invokableMethod,
                        p -> this.taskList.stream().
                                filter(t -> task.getId() == t.getId())
                                .forEach(t -> setTask(t, p, new Date(System.currentTimeMillis() + fixedDelayTmp), false))
                );
                IntervalTask intervalTask = new IntervalTask(runnable, fixedDelay, initialDelay);
                ScheduledTask scheduledTask = this.registrar.scheduleFixedDelayTask(intervalTask);
                task.addScheduledInfo(scheduledTask, bean, method);
                if (isScheduled)
                    scheduled.addScheduledInfo(scheduledTask, bean, method);
                else
                    this.taskList.add(task);
//                tasks.add(scheduledTask);
            }
            String fixedDelayString = scheduled.getFixedDelayString();
            if (StringUtils.hasText(fixedDelayString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
                }
                try {
                    fixedDelay = Long.parseLong(fixedDelayString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into integer");
                }
                long fixedDelayTmp = fixedDelay;
                Task task;
                if (isScheduled) task = scheduled;
                else
                    task = new Task(ATOMIC_INTEGER.getAndIncrement(),
                            method.toGenericString(),
                            fixedDelayTmp,
                            -1,
                            Task.State.WAITING_NEXT,
                            null,
                            scheduled.getTaskName());
                runnable = new ProgressScheduledMethodRunnable(bean,
                        invokableMethod,
                        p -> taskList.stream().
                                filter(t -> task.getId() == t.getId())
                                .forEach(t -> setTask(t, p, new Date(System.currentTimeMillis() + fixedDelayTmp), false))
                );
                IntervalTask intervalTask = new IntervalTask(runnable, fixedDelay, initialDelay);
                ScheduledTask scheduledTask = this.registrar.scheduleFixedDelayTask(intervalTask);
                task.addScheduledInfo(scheduledTask, bean, method);
                if (isScheduled)
                    scheduled.addScheduledInfo(scheduledTask, bean, method);
                else
                    this.taskList.add(task);
//                tasks.add(scheduledTask);
            }

            // Check fixed rate
            long fixedRate = scheduled.getFixedRate();
            if (fixedRate >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                final long fixedRateTmp = fixedRate;
                Task task;
                if (isScheduled) task = scheduled;
                else
                    task = new Task(ATOMIC_INTEGER.getAndIncrement(),
                            method.toGenericString(),
                            -1,
                            fixedRateTmp,
                            Task.State.WAITING_NEXT,
                            null,
                            scheduled.getTaskName());
                runnable = new ProgressScheduledMethodRunnable(bean,
                        invokableMethod,
                        p -> this.taskList.stream().
                                filter(t -> task.getId() == t.getId())
                                .forEach(t -> setTask(t, p, new Date(System.currentTimeMillis() + fixedRateTmp), true))
                );
                IntervalTask intervalTask = new IntervalTask(runnable, fixedRate, initialDelay);
                ScheduledTask scheduledTask = this.registrar.scheduleFixedRateTask(intervalTask);
                task.addScheduledInfo(scheduledTask, bean, method);
                if (isScheduled)
                    scheduled.addScheduledInfo(scheduledTask, bean, method);
                else
                    this.taskList.add(task);
//                tasks.add(scheduledTask);
            }
            String fixedRateString = scheduled.getFixedRateString();
            if (StringUtils.hasText(fixedRateString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
                }
                try {
                    fixedRate = Long.parseLong(fixedRateString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into integer");
                }
                final long fixedRateTmp = fixedRate;
                Task task;
                if (isScheduled) task = scheduled;
                else
                    task = new Task(ATOMIC_INTEGER.getAndIncrement(),
                            method.toGenericString(),
                            -1,
                            fixedRateTmp,
                            Task.State.WAITING_NEXT,
                            null,
                            scheduled.getTaskName());
                runnable = new ProgressScheduledMethodRunnable(bean,
                        invokableMethod,
                        p -> this.taskList.stream().
                                filter(t -> task.getId() == t.getId())
                                .forEach(t -> setTask(t, p, new Date(System.currentTimeMillis() + fixedRateTmp), true))
                );
                IntervalTask intervalTask = new IntervalTask(runnable, fixedRate, initialDelay);
                ScheduledTask scheduledTask = this.registrar.scheduleFixedRateTask(intervalTask);
                task.addScheduledInfo(scheduledTask, bean, method);
                if (isScheduled)
                    scheduled.addScheduledInfo(scheduledTask, bean, method);
                else
                    this.taskList.add(task);
//                tasks.add(scheduledTask);
            }

            // Check whether we had any attribute set
            Assert.isTrue(processedSchedule, errorMessage);

            // Finally register the scheduled tasks
//            synchronized (this.scheduledTasks) {
//                Set<ScheduledTask> registeredTasks = this.scheduledTasks.computeIfAbsent(bean, k -> new LinkedHashSet<>(4));
//                registeredTasks.addAll(tasks);
//            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Encountered invalid @ProgressScheduled method '" + method.getName() + "': " + ex.getMessage());
        }
    }


    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) {

//        Set<ScheduledTask> tasks;
//        synchronized (this.scheduledTasks) {
//            tasks = this.scheduledTasks.remove(bean);
//        }
//        if (tasks != null) {
//            for (ScheduledTask task : tasks) {
//
//                task.cancel();
//            }
//        }
        synchronized (taskList) {
            taskList
                    .stream()
                    .filter(task -> task.getBean().equals(bean) && task.getScheduledTask() != null)
                    .forEach(task -> task.getScheduledTask().cancel());
        }
        super.postProcessBeforeDestruction(bean, beanName);
    }

    @Override
    public boolean requiresDestruction(Object bean) {

        boolean rtn;
        rtn = super.requiresDestruction(bean);
//        synchronized (this.scheduledTasks) {
//            return this.scheduledTasks.containsKey(bean);
//        }
        synchronized (taskList) {
            rtn |= taskList
                    .stream()
                    .filter(task -> task.getBean().equals(bean))
                    .count() > 0;
        }
        return rtn;
    }

    @Override
    public void destroy() {
//        synchronized (this.scheduledTasks) {
//            Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
//            for (Set<ScheduledTask> tasks : allTasks) {
//                for (ScheduledTask task : tasks) {
//                    task.cancel();
//                }
//            }
//            this.scheduledTasks.clear();
//        }
        synchronized (this.taskList) {
            List<Task> removeTask =
                    taskList.stream()
                            .filter(task -> task.getScheduledTask() != null).collect(Collectors.toList());
            removeTask.forEach(task -> {
                task.getScheduledTask().cancel();
                taskList.remove(task);
            });
        }
        this.registrar.destroy();

        super.destroy();
    }

}
