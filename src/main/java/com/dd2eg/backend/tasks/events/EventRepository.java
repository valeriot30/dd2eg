package com.dd2eg.backend.tasks.events;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByStatusOrderByCreatedAtAsc(EventStatus status);

    List<Event> findByStatus(EventStatus status);

    long countByStatus(EventStatus status);
}
