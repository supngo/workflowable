package com.naturecode.annual_inspection.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
  public String startAnnualInspectionProcess(@RequestParam(required = true) String servicer) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("servicer", servicer);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("annualInspection", variables);
    log.log(Level.INFO, "New Task: {0} created!", processInstance.getId());
    return "New Task: " + processInstance.getId() + " created!";
  }

  @GetMapping("/tasks")
  public List<TaskDTO> getTasks(@RequestParam(required = false) String assignee) {
    List<Task> tasks = taskService.createTaskQuery().list();
    tasks.stream().forEach(task -> log.log(Level.INFO, "taskId: {0}", task.getId()));
    return tasks.stream()
        .map(task -> new TaskDTO(task.getId(), task.getName(), task.getAssignee()))
        .collect(Collectors.toList());
  }

  @PostMapping("/servicer/{taskId}")
  public ResponseEntity<String> servicer(
      @PathVariable(required = true) String taskId,
      @RequestParam(required = true) String action,
      @RequestParam(required = true) String analyst,
      @RequestParam(required = false) String comment) {
    Map<String, Object> variables = new HashMap<>();
    try {
      if (action == null || (!action.equals("submit") && !action.equals("edit"))) {
        log.log(Level.INFO, "Missing action for servicer with task: {0}", taskId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("An action is required and can either be submit or edit");
      }
      if (comment != null) {
        variables.put("comment", comment);
      }
      variables.put("analyst", analyst);
      taskService.complete(taskId, variables);
      log.log(Level.INFO, "A servicer has done ''{0}'' for task: {1}", new Object[] { action, taskId });
      return ResponseEntity.ok("A servicer has done '" + action + "' for task: " + taskId);
    } catch (FlowableObjectNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("/servicer -> Task: " + taskId + " not found.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("/servicer -> Task: " + taskId + " got error: " + e.getMessage());
    }
  }

  @PostMapping("/analyst/{taskId}")
  public ResponseEntity<String> analyst(
      @PathVariable(required = true) String taskId,
      @RequestParam(required = false) String servicer,
      @RequestParam(required = false) String manager,
      @RequestParam(required = true) String decision,
      @RequestParam(required = false) String comment) {
    Map<String, Object> variables = new HashMap<>();
    try {
      if (comment != null) {
        variables.put("comment", comment);
      }
      switch (decision) {
        case "reject", "send_back" -> {
          if (servicer == null || servicer.trim().length() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A servicer is required!");
          }
          variables.put("servicer", servicer);
        }
        case "approve" -> {
          if (manager == null || manager.trim().length() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A manager is required!");
          }
          variables.put("manager", manager);
        }
        default -> {
          log.log(Level.INFO, "Incorrect decision ''{0}'' for task: {1}", new Object[] { decision, taskId });
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("A decision either be approve, reject, or send_back");
        }
      }
      variables.put("decision", decision);
      taskService.complete(taskId, variables);
      log.log(Level.INFO, "An analyst chose to ''{0}'' for task: {1}", new Object[] { decision, taskId });
      return ResponseEntity.ok("An analyst chose to '" + decision + "' for task: " + taskId);
    } catch (FlowableObjectNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("/analyst -> Task: " + taskId + " not found.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("/analyst -> Task: " + taskId + " got error: " + e.getMessage());
    }
  }

  @PostMapping("/manager/{taskId}")
  public ResponseEntity<String> manager(
      @PathVariable(required = true) String taskId,
      @RequestParam(required = false) String servicer,
      @RequestParam(required = false) String analyst,
      @RequestParam(required = true) String decision,
      @RequestParam(required = false) String comment) {
    Map<String, Object> variables = new HashMap<>();
    try {
      if (comment != null) {
        variables.put("comment", comment);
      }

      switch (decision) {
        case "reject" -> {
          if (servicer == null || servicer.trim().length() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A servicer is required!");
          }
          variables.put("servicer", servicer);
        }
        case "send_back" -> {
          if (analyst == null || analyst.trim().length() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An analyst is required!");
          }
          variables.put("analyst", analyst);
        }
        case "approve" -> {
        }
        default -> {
          log.log(Level.INFO, "Incorrect decision ''{0}'' for task: {1}", new Object[] { decision, taskId });
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("A decision either be approve, reject, or send_back");
        }
      }
      variables.put("decision", decision);
      taskService.complete(taskId, variables);
      log.log(Level.INFO, "A manager chose to ''{0}'' for task: {1}", new Object[] { decision, taskId });
      return ResponseEntity.ok("A manager chose to '" + decision + "' for task: " + taskId);
    } catch (FlowableObjectNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("/manager -> Task: " + taskId + " not found.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("/manager -> Task: " + taskId + " got error: " + e.getMessage());
    }
  }
}
