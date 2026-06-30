package com.dd2eg.model;

import com.dd2eg.utils.EventStatus;
import com.dd2eg.utils.EventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "events")
@Sharded(shardKey = { "_id" }, shardingStrategy = ShardingStrategy.HASH)
public class Event {
    @Id
    private String id;
    private EventType type;
    private String payload;
    private EventStatus status = EventStatus.PENDING;
    private int retryCount = 0;
    private String errorMessage;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;
}
