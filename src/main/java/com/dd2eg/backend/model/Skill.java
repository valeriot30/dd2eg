package com.dd2eg.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
public class Skill {

    @Id
    private String Id = UUID.randomUUID().toString();

    private String name;
}
