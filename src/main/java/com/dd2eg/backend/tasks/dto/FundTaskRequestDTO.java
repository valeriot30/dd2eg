package com.dd2eg.backend.tasks.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FundTaskRequestDTO {
    private String taskId;
    private int amount;
}
