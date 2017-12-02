package org.springframework.schedulingX.annotation;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.SchedulingConfiguration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.lang.annotation.*;
import java.util.concurrent.Executor;

/**
 * Enables Spring's scheduled task execution capability, similar to
 * functionality found in Spring's {@code <task:*>} XML namespace. To be used
 * on @{@link Configuration} classes as follows:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 * <p>
 * // various &#064;Bean definitions
 * }</pre>
 * <p>
 * This enables detection of @{@link Scheduled} annotations on any Spring-managed
 * bean in the container. For example, given a class {@code MyTask}
 * <p>
 * <pre class="code">
 * package com.myco.tasks;
 * <p>
 * public class MyTask {
 * <p>
 * &#064;Scheduled(fixedRate=1000)
 * public void work() {
 * // task execution logic
 * }
 * }</pre>
 * <p>
 * the following configuration would ensure that {@code MyTask.work()} is called
 * once every 1000 ms:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 * <p>
 * &#064;Bean
 * public MyTask task() {
 * return new MyTask();
 * }
 * }</pre>
 * <p>
 * Alternatively, if {@code MyTask} were annotated with {@code @Component}, the
 * following configuration would ensure that its {@code @Scheduled} method is
 * invoked at the desired interval:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * &#064;ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 * }</pre>
 * <p>
 * Methods annotated with {@code @Scheduled} may even be declared directly within
 * {@code @Configuration} classes:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 * <p>
 * &#064;Scheduled(fixedRate=1000)
 * public void work() {
 * // task execution logic
 * }
 * }</pre>
 * <p>
 * <p>By default, will be searching for an associated scheduler definition: either
 * a unique {@link org.springframework.scheduling.TaskScheduler} bean in the context,
 * or a {@code TaskScheduler} bean named "taskScheduler" otherwise; the same lookup
 * will also be performed for a {@link java.util.concurrent.ScheduledExecutorService}
 * bean. If neither of the two is resolvable, a local single-threaded default
 * scheduler will be created and used within the registrar.
 * <p>
 * <p>When more control is desired, a {@code @Configuration} class may implement
 * {@link SchedulingConfigurer}. This allows access to the underlying
 * {@link ScheduledTaskRegistrar} instance. For example, the following example
 * demonstrates how to customize the {@link Executor} used to execute scheduled
 * tasks:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 * <p>
 * &#064;Override
 * public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 * taskRegistrar.setScheduler(taskExecutor());
 * }
 * <p>
 * &#064;Bean(destroyMethod="shutdown")
 * public Executor taskExecutor() {
 * return Executors.newScheduledThreadPool(100);
 * }
 * }</pre>
 * <p>
 * <p>Note in the example above the use of {@code @Bean(destroyMethod="shutdown")}.
 * This ensures that the task executor is properly shut down when the Spring
 * application context itself is closed.
 * <p>
 * <p>Implementing {@code SchedulingConfigurer} also allows for fine-grained
 * control over task registration via the {@code ScheduledTaskRegistrar}.
 * For example, the following configures the execution of a particular bean
 * method per a custom {@code Trigger} implementation:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 * <p>
 * &#064;Override
 * public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 * taskRegistrar.setScheduler(taskScheduler());
 * taskRegistrar.addTriggerTask(
 * new Runnable() {
 * public void run() {
 * myTask().work();
 * }
 * },
 * new CustomTrigger()
 * );
 * }
 * <p>
 * &#064;Bean(destroyMethod="shutdown")
 * public Executor taskScheduler() {
 * return Executors.newScheduledThreadPool(42);
 * }
 * <p>
 * &#064;Bean
 * public MyTask myTask() {
 * return new MyTask();
 * }
 * }</pre>
 * <p>
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 * <p>
 * <pre class="code">
 * {@code
 * <beans>
 * <p>
 * <task:annotation-driven scheduler="taskScheduler"/>
 * <p>
 * <task:scheduler id="taskScheduler" pool-size="42"/>
 * <p>
 * <task:scheduled-tasks scheduler="taskScheduler">
 * <task:scheduled ref="myTask" method="work" fixed-rate="1000"/>
 * </task:scheduled-tasks>
 * <p>
 * <bean id="myTask" class="com.foo.MyTask"/>
 * <p>
 * </beans>
 * }</pre>
 * <p>
 * The examples are equivalent save that in XML a <em>fixed-rate</em> period is used
 * instead of a custom <em>{@code Trigger}</em> implementation; this is because the
 * {@code task:} namespace {@code scheduled} cannot easily expose such support. This is
 * but one demonstration how the code-based approach allows for maximum configurability
 * through direct access to actual componentry.<p>
 * <p>
 * 版权所有：   y.
 * 创建日期：   17-11-29.
 * 重要说明：
 * 修订历史：
 *
 * @see Scheduled
 * @see SchedulingConfiguration
 * @see SchedulingConfigurer
 * @see ScheduledTaskRegistrar
 * @see Trigger
 * @see ScheduledAnnotationBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ProgressSchedulingConfiguration.class)
@Documented
public @interface EnableProgressScheduling {

}

