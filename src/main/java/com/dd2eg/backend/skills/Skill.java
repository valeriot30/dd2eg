package com.dd2eg.backend.skills;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
public class Skill {

    @Id
    private String Id;

    private String name;
}
