package com.dd2eg.backend.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Comment {

    private String id = UUID.randomUUID().toString();

    private String content;

    private String authorId;

    private LocalDateTime createdAt = LocalDateTime.now();

}