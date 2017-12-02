package com.example.demo.controller;

import org.springframework.schedulingX.annotation.Task;
import org.springframework.schedulingX.annotation.ProgressScheduledAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
@RestController
public class ProgressTaskController {
    private final ProgressScheduledAnnotationBeanPostProcessor progressScheduleConfig;

    @Autowired
    public ProgressTaskController(ProgressScheduledAnnotationBeanPostProcessor progressScheduleConfig) {
        this.progressScheduleConfig = progressScheduleConfig;
    }

    @PostMapping("/progressTask")
    @ResponseBody
    public Task task(@RequestBody Task task) {
        progressScheduleConfig.setTask(task);
        return task;
    }

    @GetMapping("/progressTask")
    public List<Task> doGet() {
        return progressScheduleConfig.getTasks();
    }
}
