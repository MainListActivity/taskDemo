package com.example.demo.scheduling;

import org.springframework.schedulingX.annotation.ProgressScheduled;
import org.springframework.schedulingX.listener.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
@Component
public class ProgressSchedule {
    private static final Logger log = LoggerFactory.getLogger(ProgressSchedule.class);

    @ProgressScheduled(cron = "0/5 * * * * *", name = "cron every 3s")
//    @ProgressScheduled(fixedDelay = 1000, name = "fixedDelay 1s")
    public void progressOne(ProgressListener progressListener) {
        for (int i = 0; i < 6; i++) {
            progressListener.progress(i * 100 / 5);
            for (long j = 0; j < 3234567890L; j++) ;
        }
        log.info("1 progress task executed!");
    }

    @ProgressScheduled(cron = "0/8 * * * * *")
    public void progressTwo(ProgressListener progressListener) {
        for (int i = 0; i < 10; i++) {
            progressListener.progress(i * 100 / 9);
            for (long j = 0; j < 3234567890L; j++) ;
        }
        log.info("2 progress task executed!");
    }
}
