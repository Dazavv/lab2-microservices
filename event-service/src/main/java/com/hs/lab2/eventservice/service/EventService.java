package com.hs.lab2.eventservice.service;

import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.exceptions.EventConflictException;
import com.hs.lab2.eventservice.exceptions.EventNotFoundException;
import com.hs.lab2.eventservice.repository.EventRepository;
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
    private final UserClientService userClientService;

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

        return userClientService.getUserById(ownerId)
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
