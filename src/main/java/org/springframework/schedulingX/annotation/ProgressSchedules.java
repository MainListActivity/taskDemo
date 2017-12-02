package org.springframework.schedulingX.annotation;

import java.lang.annotation.*;

/**
 * Created by y
 * on 2017/11/27
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProgressSchedules {
    ProgressScheduled[] value();
}
