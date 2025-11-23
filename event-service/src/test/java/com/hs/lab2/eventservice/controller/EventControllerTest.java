package com.hs.lab2.eventservice.controller;

import com.hs.lab2.eventservice.dto.requests.CreateEventRequest;
import com.hs.lab2.eventservice.dto.responses.EventDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.mapper.EventMapper;
import com.hs.lab2.eventservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventController eventController;

    private Event testEvent;
    private EventDto testEventDto;
    private CreateEventRequest createRequest;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(LocalDate.now().plusDays(1));
        testEvent.setStartTime(LocalTime.of(10, 0));
        testEvent.setEndTime(LocalTime.of(11, 0));
        testEvent.setOwnerId(1L);

        testEventDto = new EventDto(
                1L,
                "Test Event",
                "Test Description",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                1L
        );

        createRequest = new CreateEventRequest(
                "Test Event",
                "Test Description",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                1L
        );
    }

    @Test
    void testAddEvent() {
        when(eventService.addEvent(anyString(), anyString(), any(), any(), any(), anyLong()))
                .thenReturn(Mono.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.addEvent(createRequest))
                .expectNextMatches(response -> {
                    EventDto dto = response.getBody();
                    return dto != null &&
                            dto.id().equals(1L) &&
                            dto.name().equals("Test Event");
                })
                .verifyComplete();
    }

    @Test
    void testGetAllEvents() {
        Event event2 = new Event();
        event2.setId(2L);
        event2.setName("Event 2");

        EventDto dto2 = new EventDto(2L, "Event 2", null, null, null, null, null);

        when(eventService.getAllEvents()).thenReturn(Flux.just(testEvent, event2));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);
        when(eventMapper.toEventDto(event2)).thenReturn(dto2);

        StepVerifier.create(eventController.getAllEvents())
                .expectNext(testEventDto)
                .expectNext(dto2)
                .verifyComplete();
    }

    @Test
    void testGetEventById() {
        when(eventService.getEventById(1L)).thenReturn(Mono.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.getEventById(1L))
                .expectNextMatches(response -> {
                    EventDto dto = response.getBody();
                    return dto != null && dto.id().equals(1L);
                })
                .verifyComplete();
    }

    @Test
    void testGetUserEvents() {
        when(eventService.getUserEventsById(eq(1L), any()))
                .thenReturn(Flux.just(testEvent));

        StepVerifier.create(eventController.getUserEvents(1L, 0, 10))
                .expectNext(testEvent)
                .verifyComplete();
    }

    @Test
    void testDeleteEventById() {
        when(eventService.deleteEventById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(eventController.deleteEventById(1L))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void testGetBusyEventsForUsersBetweenDates() {
        when(eventService.getBusyEventsForUsersBetweenDates(anyList(), any(), any()))
                .thenReturn(Flux.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.getBusyEventsForUsersBetweenDates(
                java.util.List.of(1L, 2L),
                LocalDate.now(),
                LocalDate.now().plusDays(7)))
                .expectNext(testEventDto)
                .verifyComplete();
    }
}

