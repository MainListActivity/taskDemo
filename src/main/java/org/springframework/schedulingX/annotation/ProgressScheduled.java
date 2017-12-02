package org.springframework.schedulingX.annotation;


import java.lang.annotation.*;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ProgressSchedules.class)
public @interface ProgressScheduled {

    /**
     * task's name to display
     *
     * @return a task name
     */
    String name() default "";

    /**
     * A cron-like expression, extending the usual UN*X definition to include
     * triggers on the second as well as minute, hour, day of month, month
     * and day of week.  e.g. {@code "0 * * * * MON-FRI"} means once per minute on
     * weekdays (at the top of the minute - the 0th second).
     *
     * @return an expression that can be parsed to a cron schedule
     * @see org.springframework.scheduling.support.CronSequenceGenerator
     */
    String cron() default "";

    /**
     * A time zone for which the cron expression will be resolved. By default, this
     * attribute is the empty String (i.e. the server's local time zone will be used).
     *
     * @return a zone id accepted by {@link java.util.TimeZone#getTimeZone(String)},
     * or an empty String to indicate the server's default time zone
     * @see org.springframework.scheduling.support.CronTrigger#CronTrigger(String, java.util.TimeZone)
     * @see java.util.TimeZone
     * @since 4.0
     */
    String zone() default "";

    /**
     * Execute the annotated method with a fixed period in milliseconds between the
     * end of the last invocation and the start of the next.
     *
     * @return the delay in milliseconds
     */
    long fixedDelay() default -1;

    /**
     * Execute the annotated method with a fixed period in milliseconds between the
     * end of the last invocation and the start of the next.
     *
     * @return the delay in milliseconds as a String value, e.g. a placeholder
     * @since 3.2.2
     */
    String fixedDelayString() default "";

    /**
     * Execute the annotated method with a fixed period in milliseconds between
     * invocations.
     *
     * @return the period in milliseconds
     */
    long fixedRate() default -1;

    /**
     * Execute the annotated method with a fixed period in milliseconds between
     * invocations.
     *
     * @return the period in milliseconds as a String value, e.g. a placeholder
     * @since 3.2.2
     */
    String fixedRateString() default "";

    /**
     * Number of milliseconds to delay before the first execution of a
     * {@link #fixedRate()} or {@link #fixedDelay()} task.
     *
     * @return the initial delay in milliseconds
     * @since 3.2
     */
    long initialDelay() default -1;

    /**
     * Number of milliseconds to delay before the first execution of a
     * {@link #fixedRate()} or {@link #fixedDelay()} task.
     *
     * @return the initial delay in milliseconds as a String value, e.g. a placeholder
     * @since 3.2.2
     */
    String initialDelayString() default "";
}
