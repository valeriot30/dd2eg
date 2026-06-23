package com.dd2eg.backend.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
public class DeveloperInfo {
    private double rating = 0.0;

    private List<DevReport> devReports = new ArrayList<>();
}
