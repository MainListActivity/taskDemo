package com.example.demo.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.schedulingX.annotation.Task;
import org.springframework.schedulingX.listener.TaskUpdateListener;
import org.springframework.stereotype.Service;


/**
 * 版权所有：   y.
 * 创建日期：   17-11-24.
 * 重要说明：
 * 修订历史：
 */
@Service
public class TaskListenerHandler implements TaskUpdateListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
//    private final SimpMessagingTemplate websocket;
//
//    @Autowired
//    public TaskListenerHandler(SimpMessagingTemplate websocket) {
//        this.websocket = websocket;
//    }

    @Override
    public void onUpdate(Task task) {
        logger.info(task.getId() + "--" + task.getProgress());
    }
}
