package com.hs.lab2.eventservice.controller;

import com.hs.lab2.eventservice.dto.requests.CreateEventRequest;
import com.hs.lab2.eventservice.dto.responses.EventDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.mapper.EventMapper;
import com.hs.lab2.eventservice.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final EventMapper eventMapper;

    @PostMapping
    public Mono<ResponseEntity<EventDto>> addEvent(@Valid @RequestBody CreateEventRequest request) {
        Mono<Event> event = eventService.addEvent(
                request.name(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.ownerId());
        return event.map(eventMapper::toEventDto)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<EventDto> getAllEvents() {
        return eventService.getAllEvents()
                .map(eventMapper::toEventDto);
    }

    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<EventDto>> getEventById(@PathVariable @Min(1) Long id) {
        return eventService.getEventById(id)
                .map(eventMapper::toEventDto)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(path = "/{id}")
    public Mono<ResponseEntity<Void>> deleteEventById(@PathVariable @Min(1) Long id) {
        return eventService.deleteEventById(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/api/v1/event/busy")
    public Flux<EventDto> getBusyEventsForUsersBetweenDates(
            @RequestParam List<Long> userIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return eventService.getBusyEventsForUsersBetweenDates(userIds, startDate, endDate)
                .map(eventMapper::toEventDto);
    }
}
