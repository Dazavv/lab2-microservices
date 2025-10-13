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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/groupEvent")
@RequiredArgsConstructor
public class GroupEventController {
    private final GroupEventService groupEventService;
    private final GroupEventMapper groupEventMapper;
    private final RecommendationService recommendationService;

    @PostMapping
    public Mono<ResponseEntity<GroupEventDto>> addGroupEvent(@RequestBody CreateGroupEventRequest request) {
        return groupEventService.addGroupEvent(
                        request.name(),
                        request.description(),
                        request.participantIds(),
                        request.ownerId()
                )
                .map(groupEventMapper::toGroupEventDto)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }


    @GetMapping
    public Flux<GroupEventDto> getAllGroupEvents() {
        return groupEventService.getAllGroupEvents()
                .map(groupEventMapper::toGroupEventDto);
    }

    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<GroupEventDto>> getGroupEventById(@PathVariable @Min(1) Long id) {
        return groupEventService.getGroupEventById(id)
                .map(groupEventMapper::toGroupEventDto)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(path = "/{id}")
    public Mono<ResponseEntity<Void>> deleteGroupEventById(@PathVariable @Min(1) Long id) {
        return groupEventService.deleteGroupEventById(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping(path = "/recommend")
    public Flux<RecommendTimeSlotDto> recommendGroupEvents(@RequestBody RecommendSlotsRequest request) {
        return recommendationService.recommendSlots(
                request.periodStart(),
                request.periodEnd(),
                request.duration(),
                request.groupEventId()
        );
    }

    @PostMapping(path = "/book")
    public Mono<ResponseEntity<GroupEventDto>> bookGroupEvent(@Valid @RequestBody BookSlotRequest request) {
        return recommendationService.bookSlot(
                request.groupEventId(), request.date(), request.startTime(), request.endTime()
                )
                .map(groupEventMapper::toGroupEventDto)
                .map(ResponseEntity::ok);
    }
}
