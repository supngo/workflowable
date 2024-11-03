package com.naturecode.annual_inspection.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaskDTO {
  private String id;
  private String name;
  private String assignee;
}
