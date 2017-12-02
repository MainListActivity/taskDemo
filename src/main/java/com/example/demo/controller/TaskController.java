package com.example.demo.controller;

import com.example.demo.config.ScheduleConfig;
import org.springframework.schedulingX.annotation.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 版权所有：   y.
 * 创建日期：   17-11-23.
 * 重要说明：
 * 修订历史：
 */
@RestController
public class TaskController {
    private final ScheduleConfig scheduleConfig;

    @Autowired
    public TaskController(ScheduleConfig scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
    }

    @PostMapping("/task")
    public String task(@RequestBody Task task) {
        scheduleConfig.editTask(task);
        return task.getState().name();
    }

    @GetMapping("/task")
    public List<Task> doGet() {
        return scheduleConfig.getTaskList();
    }
}
