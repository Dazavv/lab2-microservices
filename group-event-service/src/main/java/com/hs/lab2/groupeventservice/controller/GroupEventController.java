package com.hs.lab2.groupeventservice.controller;

import com.hs.lab2.groupeventservice.dto.requests.BookSlotRequest;
import com.hs.lab2.groupeventservice.dto.requests.CreateGroupEventRequest;
import com.hs.lab2.groupeventservice.dto.requests.RecommendSlotsRequest;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.dto.responses.RecommendTimeSlotDto;
import com.hs.lab2.groupeventservice.mapper.GroupEventMapper;
import com.hs.lab2.groupeventservice.service.GroupEventService;
import com.hs.lab2.groupeventservice.service.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/group-event")
@RequiredArgsConstructor
public class GroupEventController {

    private final GroupEventService groupEventService;
    private final GroupEventMapper groupEventMapper;
    private final RecommendationService recommendationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Mono<GroupEventDto> addGroupEvent(@RequestBody CreateGroupEventRequest request) {
        return groupEventService.addGroupEvent(
                        request.name(),
                        request.description(),
                        request.participantIds(),
                        request.ownerId()
                )
                .map(groupEventMapper::toGroupEventDto);
    }

    @GetMapping
    public Flux<GroupEventDto> getAllGroupEvents() {
        return groupEventService.getAllGroupEvents()
                .map(groupEventMapper::toGroupEventDto);
    }

    @GetMapping("/{id}")
    public Mono<GroupEventDto> getGroupEventById(@PathVariable @Min(1) Long id) {
        return groupEventService.getGroupEventById(id)
                .map(groupEventMapper::toGroupEventDto);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public Mono<Void> deleteGroupEventById(@PathVariable @Min(1) Long id) {
        return groupEventService.deleteGroupEventById(id).then();
    }

    @PostMapping("/recommend")
    public Flux<RecommendTimeSlotDto> recommendGroupEvents(@RequestBody RecommendSlotsRequest request) {
        return recommendationService.recommendSlots(
                request.periodStart(),
                request.periodEnd(),
                request.duration(),
                request.groupEventId()
        );
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/book")
    public Mono<GroupEventDto> bookGroupEvent(@Valid @RequestBody BookSlotRequest req) {
        return recommendationService.bookSlot(
                req.groupEventId(),
                req.date(),
                req.startTime(),
                req.endTime()
        );
    }
}
