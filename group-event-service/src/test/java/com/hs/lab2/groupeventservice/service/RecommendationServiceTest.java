package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.dto.responses.EventDto;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.mapper.GroupEventMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GroupEventService groupEventService;

    @Mock
    private GroupEventMapper groupEventMapper;

    @Mock
    private EventClient eventClient;

    @Mock
    private GroupEventLoader loader;

    @InjectMocks
    private RecommendationService recommendationService;

    private GroupEvent testGroupEvent;

    @BeforeEach
    void setUp() {
        testGroupEvent = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .participantIds(List.of(2L, 3L))
                .ownerId(1L)
                .status(GroupEventStatus.PENDING)
                .build();
    }

    @Test
    void testRecommendSlots_Success() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);
        Duration duration = Duration.ofHours(1);

        when(loader.loadParticipantIds(1L))
                .thenReturn(List.of(2L, 3L));

        // свободных слотов будет достаточно, если занятость пустая
        when(eventClient.getBusyEventsForUsersBetweenDates(anyList(), anyString(), anyString()))
                .thenReturn(Flux.empty());

        StepVerifier.create(recommendationService.recommendSlots(startDate, endDate, duration, 1L))
                .expectNextCount(5) // первые 5 слотов
                .verifyComplete();

        verify(loader).loadParticipantIds(1L);
        verify(eventClient)
                .getBusyEventsForUsersBetweenDates(anyList(), eq(startDate.toString()), eq(endDate.toString()));
    }

    @Test
    void testFetchBusyIntervals_Success() {
        List<Long> participantIds = List.of(2L, 3L);
        String start = "2024-01-01";
        String end = "2024-01-07";

        EventDto event1 = new EventDto(
                1L,
                "Event1",
                null,
                LocalDate.parse("2024-01-02"),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                2L
        );
        EventDto event2 = new EventDto(
                2L,
                "Event2",
                null,
                LocalDate.parse("2024-01-03"),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                3L
        );

        when(eventClient.getBusyEventsForUsersBetweenDates(participantIds, start, end))
                .thenReturn(Flux.just(event1, event2));

        StepVerifier.create(recommendationService.fetchBusyIntervals(participantIds, start, end))
                .expectNextMatches(interval ->
                        interval.date().equals(LocalDate.parse("2024-01-02")) &&
                                interval.start().equals(LocalTime.of(10, 0)) &&
                                interval.end().equals(LocalTime.of(11, 0))
                )
                .expectNextMatches(interval ->
                        interval.date().equals(LocalDate.parse("2024-01-03")) &&
                                interval.start().equals(LocalTime.of(14, 0)) &&
                                interval.end().equals(LocalTime.of(15, 0))
                )
                .verifyComplete();

        verify(eventClient).getBusyEventsForUsersBetweenDates(participantIds, start, end);
    }

    @Test
    void testBookSlot_Success() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        GroupEvent existing = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .participantIds(List.of(2L, 3L))
                .ownerId(1L)
                .status(GroupEventStatus.PENDING)
                .build();

        GroupEvent saved = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .participantIds(List.of(2L, 3L))
                .ownerId(1L)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .status(GroupEventStatus.CONFIRMED)
                .build();

        GroupEventDto savedDto = new GroupEventDto(
                1L,
                "Test Group Event",
                null,
                date,
                startTime,
                endTime,
                List.of(2L, 3L),
                1L,
                GroupEventStatus.CONFIRMED
        );

        when(groupEventService.getGroupEventById(1L))
                .thenReturn(Mono.just(existing));
        when(groupEventService.saveGroupEvent(any(GroupEvent.class)))
                .thenReturn(Mono.just(saved));
        when(groupEventMapper.toGroupEventDto(saved))
                .thenReturn(savedDto);

        StepVerifier.create(recommendationService.bookSlot(1L, date, startTime, endTime))
                .expectNextMatches(dto ->
                        dto.status() == GroupEventStatus.CONFIRMED &&
                                dto.date().equals(date) &&
                                dto.startTime().equals(startTime) &&
                                dto.endTime().equals(endTime)
                )
                .verifyComplete();

        verify(groupEventService).getGroupEventById(1L);
        verify(groupEventService).saveGroupEvent(any(GroupEvent.class));
        verify(groupEventMapper).toGroupEventDto(saved);
    }

    @Test
    void testBookSlot_EventNotFound() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        when(groupEventService.getGroupEventById(1L))
                .thenReturn(Mono.error(new EventNotFoundException("Group event not found")));

        StepVerifier.create(recommendationService.bookSlot(1L, date, startTime, endTime))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(groupEventService).getGroupEventById(1L);
        verify(groupEventService, never()).saveGroupEvent(any(GroupEvent.class));
        verifyNoInteractions(eventClient);
    }
}
