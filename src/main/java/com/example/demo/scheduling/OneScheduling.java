package com.example.demo.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by y
 * on 2017/11/22
 */
@Component
public class OneScheduling {
    private static final Logger log = LoggerFactory.getLogger(OneScheduling.class);

    @Scheduled(cron = "0/3 * * * * *")
    public void one() throws InterruptedException {
//        Thread.sleep(5000);
//        log.info("one execute");
    }
}
