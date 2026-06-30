package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportedDeveloperDTO {
    private String id;
    private String name;
    private String profilePic;
    private double score;
    private int numReports;
}
