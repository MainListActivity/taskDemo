package org.springframework.schedulingX.annotation;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.scheduling.config.ScheduledTask;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-22.
 * 重要说明：
 * 修订历史：
 */
public class Task {
    private long id;
    private String triggerName;
    private String cron;
    private long fixedDelay;
    private long fixedRate;
    private State state;
    private Date nextExecute;
    private Date lastExecute;
    private int progress;
    private String taskName;
    private long initialDelay;
    private String zone;
    private String initialDelayString;
    private String fixedRateString;
    private String fixedDelayString;


    @JsonIgnore
    private ScheduledTask scheduledTask;
    @JsonIgnore
    private Object bean;
    @JsonIgnore
    private Method method;


    public Task() {
    }

    public Task(long id, String triggerName, String cron, State state, Date nextExecute) {
        this(id, triggerName, cron, state, nextExecute, "");
    }

    public Task(long id, String triggerName, String cron, State state, Date nextExecute, String taskName) {
        this(id, triggerName, cron, -1, -1, state, nextExecute, null, 0, taskName);
    }

    public Task(long id, String triggerName, long fixedDelay, long fixedRate, State state, Date nextExecute, String taskName) {
        this(id, triggerName, "", fixedDelay, fixedRate, state, nextExecute, null, 0, taskName);
    }


    public Task(long id, String triggerName, String cron, long fixedDelay, long fixedRate, State state, Date nextExecute, Date lastExecute, int progress, String taskName) {
        this(id, triggerName, cron, fixedDelay, fixedRate, state, nextExecute, lastExecute, progress, taskName, null, null, null, -1);
    }

    public Task(long id, String triggerName, String cron, long fixedDelay, long fixedRate, State state, Date nextExecute, Date lastExecute, int progress, String taskName, ScheduledTask scheduledTask, Object bean, Method method, long initialDelay) {
        this.id = id;
        this.triggerName = triggerName;
        this.cron = cron;
        this.fixedDelay = fixedDelay;
        this.fixedRate = fixedRate;
        this.state = state;
        this.nextExecute = nextExecute;
        this.lastExecute = lastExecute;
        this.progress = progress;
        this.taskName = taskName;
        this.scheduledTask = scheduledTask;
        this.bean = bean;
        this.method = method;
        this.initialDelay = initialDelay;
    }

    public void copyValueFromTask(Task task) {
        this.fixedRateString = task.getFixedRateString();
        this.fixedRate = task.getFixedRate();
        this.fixedDelayString = task.getFixedDelayString();
        this.fixedDelay = task.getFixedDelay();
        this.taskName = task.getTaskName();
        this.zone = task.getZone();
        this.initialDelay = task.getInitialDelay();
        this.initialDelayString = task.getInitialDelayString();
        this.cron = task.getCron();
        this.id = task.getId();
        this.lastExecute = task.getLastExecute();
        this.nextExecute = task.getNextExecute();
        this.progress = task.getProgress();
        this.state = task.getState();
        this.triggerName = task.getTriggerName();
//        this.method = task.getMethod();
//        this.bean = task.getBean();
//        this.scheduledTask = task.getScheduledTask();
    }

    public void copyValueFromProgressScheduled(ProgressScheduled progressScheduled) {
        if (progressScheduled != null) {
            this.cron = progressScheduled.cron();
            this.initialDelay = progressScheduled.initialDelay();
            this.initialDelayString = progressScheduled.initialDelayString();
            this.fixedDelay = progressScheduled.fixedDelay();
            this.fixedDelayString = progressScheduled.fixedDelayString();
            this.fixedRate = progressScheduled.fixedRate();
            this.fixedRateString = progressScheduled.fixedRateString();
            this.zone = progressScheduled.zone();
            this.taskName = progressScheduled.name();
        }
    }

    public void addScheduledInfo(ScheduledTask scheduledTask, Object bean, Method method) {
        this.scheduledTask = scheduledTask;
        this.bean = bean;
        this.method = method;
    }

    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            state = State.STOP;
            nextExecute=null;
        }
    }

    public String getInitialDelayString() {
        return initialDelayString;
    }

    public void setInitialDelayString(String initialDelayString) {
        this.initialDelayString = initialDelayString;
    }

    public String getFixedRateString() {
        return fixedRateString;
    }

    public void setFixedRateString(String fixedRateString) {
        this.fixedRateString = fixedRateString;
    }

    public String getFixedDelayString() {
        return fixedDelayString;
    }

    public void setFixedDelayString(String fixedDelayString) {
        this.fixedDelayString = fixedDelayString;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    ScheduledTask getScheduledTask() {
        return scheduledTask;
    }

    public void setScheduledTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }


    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public long getFixedRate() {
        return fixedRate;
    }

    public void setFixedRate(long fixedRate) {
        this.fixedRate = fixedRate;
    }

    public Date getLastExecute() {
        return lastExecute;
    }

    public void setLastExecute(Date lastExecute) {
        this.lastExecute = lastExecute;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }


    public Date getNextExecute() {
        return nextExecute;
    }

    public void setNextExecute(Date nextExecute) {
        this.nextExecute = nextExecute;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        RUN, WAITING_NEXT, STOP
    }
}
