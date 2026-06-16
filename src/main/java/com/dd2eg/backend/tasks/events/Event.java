package com.dd2eg.backend.tasks.events;
import lombok.Getter;
import lombok.Setter;
import org.bson.json.JsonObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "events")
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
