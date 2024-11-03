package com.naturecode.annual_inspection.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
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

import lombok.extern.java.Log;

@RestController
@RequestMapping("/inspection")
@Log
public class AnnualInspectionController {

  private final RuntimeService runtimeService;
  private final TaskService taskService;

  public AnnualInspectionController(RuntimeService runtimeService, TaskService taskService) {
    this.runtimeService = runtimeService;
    this.taskService = taskService;
  }

  @PostMapping("/start")
  public TaskDTO startAnnualInspectionProcess(@RequestParam String servicer) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("servicer", servicer); // Assign the servicer role

    runtimeService.startProcessInstanceByKey("annualInspection", variables);
    Task task = taskService.createTaskQuery().taskAssignee(servicer).singleResult();
    TaskDTO newTask = new TaskDTO(task.getId(), task.getName(), servicer);
    return newTask;
  }

  @GetMapping("/tasks")
  public List<TaskDTO> getTasks(@RequestParam(required = false) String assignee) {
    List<Task> tasks = taskService.createTaskQuery().list();
    return tasks.stream()
        .map(task -> new TaskDTO(task.getId(), task.getName(), task.getAssignee()))
        .collect(Collectors.toList());
  }

  @PostMapping("/complete/{taskId}")
  public ResponseEntity<String> completeTask(
        @PathVariable(required = true) String taskId, 
        @RequestParam(required = false) String analyst, 
        @RequestParam(required = false) String manager, 
        @RequestParam(required = false) String decision, 
        @RequestParam(required = false) String comment) {
    Map<String, Object> variables = new HashMap<>();
    try {
      if (comment != null) {
        variables.put("comment", comment);
      }
      if (decision != null) {
        variables.put("decision", decision);
      }
      if (analyst != null) {
        variables.put("analyst", analyst);
      }
      if (manager != null) {
        variables.put("manager", manager);
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
