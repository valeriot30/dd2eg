package com.dd2eg.backend.tasks.sponsorship;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SponsorshipDTO {
    private String enterpriseId;
    private String name;
    private Integer amount;
}
