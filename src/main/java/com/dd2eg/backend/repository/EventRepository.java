package com.dd2eg.backend.repository;

import com.dd2eg.backend.model.Event;
import com.dd2eg.backend.utils.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByStatusOrderByCreatedAtAsc(EventStatus status);

    List<Event> findByStatus(EventStatus status);

    long countByStatus(EventStatus status);
}
