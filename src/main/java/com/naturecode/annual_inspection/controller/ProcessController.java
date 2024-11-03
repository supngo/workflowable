package com.naturecode.annual_inspection.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naturecode.annual_inspection.model.TaskDTO;

@RestController
@RequestMapping("/process")
public class ProcessController {

  private final RuntimeService runtimeService;
  private final TaskService taskService;

  public ProcessController(RuntimeService runtimeService, TaskService taskService) {
    this.runtimeService = runtimeService;
    this.taskService = taskService;
  }

  @PostMapping("/start")
  public TaskDTO startProcess(@RequestParam String assignee) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("assignee", assignee); // Set the assignee for the user task
    runtimeService.startProcessInstanceByKey("simpleProcess", variables);
    Task task = taskService.createTaskQuery().taskAssignee(assignee).singleResult();
    TaskDTO newTask = new TaskDTO(task.getId(), task.getName(), assignee);
    return newTask;
  }

  @GetMapping("/tasks")
  public List<TaskDTO> getTasks() {
    List<Task> tasks = taskService.createTaskQuery().list();
    return tasks.stream()
        .map(task -> new TaskDTO(task.getId(), task.getName(), task.getAssignee()))
        .collect(Collectors.toList());
  }

  @PostMapping("/complete/{taskId}")
  public ResponseEntity<String> completeTask(@PathVariable(required = true) String taskId, @RequestParam(required = false) String comment) {
    Map<String, Object> variables = new HashMap<>();
    try {
      if (comment != null) {
        variables.put("comment", comment);
      }
      taskService.complete(taskId, variables);
      return ResponseEntity.ok("Task with ID " + taskId + " completed.");
    } catch (FlowableObjectNotFoundException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task with ID " + taskId + " not found.");
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
  }
}
