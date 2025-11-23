package com.hs.lab2.groupeventservice.controller;

import com.hs.lab2.groupeventservice.dto.requests.BookSlotRequest;
import com.hs.lab2.groupeventservice.dto.requests.CreateGroupEventRequest;
import com.hs.lab2.groupeventservice.dto.requests.RecommendSlotsRequest;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.dto.responses.RecommendTimeSlotDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.mapper.GroupEventMapper;
import com.hs.lab2.groupeventservice.service.GroupEventService;
import com.hs.lab2.groupeventservice.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupEventControllerTest {

    @Mock
    private GroupEventService groupEventService;

    @Mock
    private GroupEventMapper groupEventMapper;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private GroupEventController groupEventController;

    private GroupEvent testGroupEvent;
    private GroupEventDto testGroupEventDto;
    private CreateGroupEventRequest createRequest;

    @BeforeEach
    void setUp() {
        testGroupEvent = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .description("Test Description")
                .participantIds(List.of(2L, 3L))
                .ownerId(1L)
                .status(GroupEventStatus.PENDING)
                .build();

        testGroupEventDto = new GroupEventDto(
                1L,
                "Test Group Event",
                "Test Description",
                null,
                null,
                null,
                List.of(2L, 3L),
                1L,
                GroupEventStatus.PENDING
        );

        createRequest = new CreateGroupEventRequest(
                "Test Group Event",
                "Test Description",
                List.of(2L, 3L),
                1L
        );
    }

    @Test
    void testAddGroupEvent() {
        when(groupEventService.addGroupEvent(anyString(), anyString(), anyList(), anyLong()))
                .thenReturn(Mono.just(testGroupEvent));
        when(groupEventMapper.toGroupEventDto(testGroupEvent)).thenReturn(testGroupEventDto);

        StepVerifier.create(groupEventController.addGroupEvent(createRequest))
                .expectNextMatches(response -> {
                    GroupEventDto dto = response.getBody();
                    return dto != null &&
                            response.getStatusCode().value() == 201 &&
                            dto.id().equals(1L) &&
                            dto.name().equals("Test Group Event");
                })
                .verifyComplete();
    }

    @Test
    void testGetAllGroupEvents() {
        GroupEvent event2 = GroupEvent.builder()
                .id(2L)
                .name("Event 2")
                .build();

        GroupEventDto dto2 = new GroupEventDto(2L, "Event 2", null, null, null, null, null, null, null);

        when(groupEventService.getAllGroupEvents()).thenReturn(Flux.just(testGroupEvent, event2));
        when(groupEventMapper.toGroupEventDto(testGroupEvent)).thenReturn(testGroupEventDto);
        when(groupEventMapper.toGroupEventDto(event2)).thenReturn(dto2);

        StepVerifier.create(groupEventController.getAllGroupEvents())
                .expectNext(testGroupEventDto)
                .expectNext(dto2)
                .verifyComplete();
    }

    @Test
    void testGetGroupEventById() {
        when(groupEventService.getGroupEventById(1L)).thenReturn(Mono.just(testGroupEvent));
        when(groupEventMapper.toGroupEventDto(testGroupEvent)).thenReturn(testGroupEventDto);

        StepVerifier.create(groupEventController.getGroupEventById(1L))
                .expectNextMatches(response -> {
                    GroupEventDto dto = response.getBody();
                    return dto != null && dto.id().equals(1L);
                })
                .verifyComplete();
    }

    @Test
    void testDeleteGroupEventById() {
        when(groupEventService.deleteGroupEventById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(groupEventController.deleteGroupEventById(1L))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void testRecommendGroupEvents() {
        RecommendSlotsRequest request = new RecommendSlotsRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                Duration.ofHours(1),
                1L
        );

        RecommendTimeSlotDto slot1 = new RecommendTimeSlotDto(
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        RecommendTimeSlotDto slot2 = new RecommendTimeSlotDto(
                LocalDate.now().plusDays(2),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(recommendationService.recommendSlots(any(), any(), any(), anyLong()))
                .thenReturn(Flux.just(slot1, slot2));

        StepVerifier.create(groupEventController.recommendGroupEvents(request))
                .expectNext(slot1)
                .expectNext(slot2)
                .verifyComplete();
    }

    @Test
    void testBookGroupEvent() {
        BookSlotRequest request = new BookSlotRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        GroupEventDto bookedDto = new GroupEventDto(
                1L,
                "Test Group Event",
                "Test Description",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                List.of(2L, 3L),
                1L,
                GroupEventStatus.CONFIRMED
        );

        when(recommendationService.bookSlot(anyLong(), any(), any(), any()))
                .thenReturn(Mono.just(bookedDto));

        StepVerifier.create(groupEventController.bookGroupEvent(request))
                .expectNextMatches(response -> {
                    GroupEventDto dto = response.getBody();
                    return dto != null &&
                            dto.status() == GroupEventStatus.CONFIRMED &&
                            dto.date() != null;
                })
                .verifyComplete();
    }
}

