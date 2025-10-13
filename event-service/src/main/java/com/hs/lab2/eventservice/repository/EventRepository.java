package com.hs.lab2.eventservice.repository;


import com.hs.lab2.eventservice.entity.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    @Query("""
        SELECT * FROM events 
        WHERE owner_id IN (:userIds) 
        AND date BETWEEN :startDate AND :endDate
    """)
    Flux<Event> findBusyEventsForUsersBetweenDates(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    );
}
