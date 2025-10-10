package com.hs.lab2.eventservice.repository;


import com.hs.lab2.eventservice.entity.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;

public interface EventRepository extends ReactiveCrudRepository<Event, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
        FROM events
        WHERE owner_id = :ownerId
          AND date = :date
          AND start_time < :endTime
          AND end_time > :startTime
    """)
    Mono<Boolean> existsByOwnerAndDateAndTimeOverlap(Long ownerId,
                                                     LocalDate date,
                                                     LocalTime startTime,
                                                     LocalTime endTime);
}
