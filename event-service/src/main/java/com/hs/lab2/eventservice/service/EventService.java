package com.hs.lab2.eventservice.service;

import com.hs.lab2.eventservice.client.UserClient;
import com.hs.lab2.eventservice.dto.responses.UserDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.exceptions.EventConflictException;
import com.hs.lab2.eventservice.exceptions.EventNotFoundException;
import com.hs.lab2.eventservice.exceptions.UserNotFoundException;
import com.hs.lab2.eventservice.repository.EventRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final UserClient userClient;

    public Flux<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Mono<Event> addEvent(String name,
                                String description,
                                LocalDate date,
                                LocalTime startTime,
                                LocalTime endTime,
                                Long ownerId) {
        if (endTime.isBefore(startTime) || date.isBefore(LocalDate.now()) || endTime.equals(startTime)) {
            return Mono.error(new EventConflictException("Invalid event time"));
        }

        return getUserByIdWithCircuitBreaker(ownerId)
                .flatMap(user ->
                        eventRepository.existsByOwnerAndDateAndTimeOverlap(ownerId, date, startTime, endTime)
                                .flatMap(conflict -> {
                                    if (conflict)
                                        return Mono.error(new EventConflictException("User already has an event at this time"));
                                    Event event = new Event();
                                    event.setName(name);
                                    event.setDescription(description);
                                    event.setDate(date);
                                    event.setStartTime(startTime);
                                    event.setEndTime(endTime);
                                    event.setOwnerId(ownerId);
                                    return eventRepository.save(event);
                                })
                );
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    private Mono<UserDto> getUserByIdWithCircuitBreaker(Long ownerId) {
        return userClient.getUserById(ownerId)
                .onErrorResume(FeignException.NotFound.class, e ->
                        Mono.error(new UserNotFoundException("User with id = " + ownerId + " not found"))
                )
                .onErrorResume(FeignException.class, e ->
                        Mono.error(new RuntimeException("User-service error: " + e.status() + " " + e.getMessage()))
                );
    }

    public Mono<Event> getEventById(Long id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event with id = " + id + " not found")));
    }

    public Mono<Void> deleteEventById(Long id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event with id = " + id + " not found")))
                .flatMap(event -> eventRepository.deleteById(event.getId()));
    }
    public Flux<Event> getBusyEventsForUsersBetweenDates(List<Long> userIds,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        return eventRepository.findBusyEventsForUsersBetweenDates(userIds, startDate, endDate);
    }

    private Mono<Event> userFallback(String name,
                                     String description,
                                     LocalDate date,
                                     LocalTime startTime,
                                     LocalTime endTime,
                                     Long ownerId,
                                     Throwable t) {
        return Mono.error(new RuntimeException("User-service unavailable, try later"));
    }

    public Flux<Event> getUserEventsById(Long ownerId, Pageable pageable) {
        long limit = pageable.getPageSize();
        long offset = pageable.getOffset();

        return eventRepository.findByOwnerIdPaged(ownerId, limit, offset);
    }

    public Mono<Page<Event>> getUserEventsPage(Long ownerId, Pageable pageable) {
        return eventRepository.countByOwnerId(ownerId)
                .flatMap(total -> eventRepository.findByOwnerIdPaged(ownerId, pageable.getPageSize(), pageable.getOffset())
                        .collectList()
                        .map(events -> new PageImpl<>(events, pageable, total))
                );
    }
}
