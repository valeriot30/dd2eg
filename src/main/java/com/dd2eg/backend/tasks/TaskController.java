package com.dd2eg.backend.tasks;

import com.dd2eg.backend.tasks.dto.CreateTaskDTO;
import com.dd2eg.backend.tasks.dto.FundTaskRequestDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/{taskId}/fund")
    public Task fundTask(
            @PathVariable String taskId,
            @RequestBody FundTaskRequestDTO request
    ) {
        return taskService.fundTask(taskId, request);
    }

    @GetMapping("/{projectId}")
    public List<Task> getTasks(@PathVariable String projectId) {
        return taskService.getTasksByProjectId(projectId);
    }

    @PostMapping("/add")
    public Task createTask(@RequestBody CreateTaskDTO request) {
        return taskService.createTask(request);
    }

}
