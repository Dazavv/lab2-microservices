package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.dto.responses.EventDto;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.mapper.GroupEventMapper;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GroupEventRepository groupEventRepository;

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

        when(loader.loadParticipantIds(1L)).thenReturn(List.of(2L, 3L));

        when(eventClient.getBusyEventsForUsersBetweenDates(anyList(), anyString(), anyString()))
                .thenReturn(Flux.empty());

        StepVerifier.create(recommendationService.recommendSlots(startDate, endDate, duration, 1L))
                .expectNextCount(5)
                .verifyComplete();

        verify(loader).loadParticipantIds(1L);
    }

    @Test
    void testFetchBusyIntervals_Success() {
        List<Long> participantIds = List.of(2L, 3L);
        String start = "2024-01-01";
        String end = "2024-01-07";

        EventDto event1 = new EventDto(1L, "Event1", null, LocalDate.parse("2024-01-02"),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 2L);
        EventDto event2 = new EventDto(2L, "Event2", null, LocalDate.parse("2024-01-03"),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 3L);

        when(eventClient.getBusyEventsForUsersBetweenDates(participantIds, start, end))
                .thenReturn(Flux.just(event1, event2));

        StepVerifier.create(recommendationService.fetchBusyIntervals(participantIds, start, end))
                .expectNextMatches(interval -> interval.date().equals(LocalDate.parse("2024-01-02")))
                .expectNextMatches(interval -> interval.date().equals(LocalDate.parse("2024-01-03")))
                .verifyComplete();
    }

    @Test
    void testBookSlot_Success() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        GroupEvent bookedEvent = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .participantIds(List.of(2L, 3L))
                .ownerId(1L)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .status(GroupEventStatus.CONFIRMED)
                .build();

        GroupEventDto bookedDto = new GroupEventDto(
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

        when(groupEventRepository.findByIdWithParticipants(1L))
                .thenReturn(Optional.of(testGroupEvent));
        when(groupEventRepository.save(any(GroupEvent.class))).thenReturn(bookedEvent);
        when(groupEventMapper.toGroupEventDto(bookedEvent)).thenReturn(bookedDto);

        StepVerifier.create(recommendationService.bookSlot(1L, date, startTime, endTime))
                .expectNextMatches(dto -> {
                    return dto.status() == GroupEventStatus.CONFIRMED &&
                            dto.date().equals(date) &&
                            dto.startTime().equals(startTime) &&
                            dto.endTime().equals(endTime);
                })
                .verifyComplete();

        verify(groupEventRepository).save(any(GroupEvent.class));
    }

    @Test
    void testBookSlot_EventNotFound() {
        when(groupEventRepository.findByIdWithParticipants(1L))
                .thenReturn(Optional.empty());

        StepVerifier.create(recommendationService.bookSlot(1L, LocalDate.now(), LocalTime.now(), LocalTime.now().plusHours(1)))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(groupEventRepository, never()).save(any(GroupEvent.class));
    }
}

