package org.springframework.schedulingX.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * Created by y
 * on 2017/11/27
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProgressSchedulingConfiguration {

    @Bean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ProgressScheduledAnnotationBeanPostProcessor scheduledAnnotationProcessor() {
        return new ProgressScheduledAnnotationBeanPostProcessor();
    }
}
