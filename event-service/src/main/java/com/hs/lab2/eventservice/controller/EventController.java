package com.hs.lab2.eventservice.controller;

import com.hs.lab2.eventservice.dto.requests.CreateEventRequest;
import com.hs.lab2.eventservice.dto.responses.EventDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.mapper.EventMapper;
import com.hs.lab2.eventservice.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PostMapping
    public Mono<EventDto> addEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.addEvent(
                        request.name(),
                        request.description(),
                        request.date(),
                        request.startTime(),
                        request.endTime(),
                        request.ownerId()
                )
                .map(eventMapper::toEventDto);
    }

    @GetMapping
    public Flux<EventDto> getAllEvents() {
        return eventService.getAllEvents()
                .map(eventMapper::toEventDto);
    }

    @GetMapping("/{id}")
    public Mono<EventDto> getEventById(@PathVariable @Min(1) Long id) {
        return eventService.getEventById(id)
                .map(eventMapper::toEventDto);
    }

    @GetMapping("/owner/{id}")
    public Flux<Event> getUserEvents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return eventService.getUserEventsById(id, pageable);
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public Mono<Void> deleteEventById(@PathVariable @Min(1) Long id) {
        return eventService.deleteEventById(id).then();
    }

    @GetMapping("/busy")
    public Flux<EventDto> getBusyEventsForUsersBetweenDates(
            @RequestParam List<Long> userIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return eventService.getBusyEventsForUsersBetweenDates(userIds, startDate, endDate)
                .map(eventMapper::toEventDto);
    }
}
