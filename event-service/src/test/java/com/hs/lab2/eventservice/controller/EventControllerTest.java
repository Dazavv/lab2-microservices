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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(date);
        testEvent.setStartTime(start);
        testEvent.setEndTime(end);
        testEvent.setOwnerId(1L);

        testEventDto = new EventDto(
                1L,
                "Test Event",
                "Test Description",
                date,
                start,
                end,
                1L
        );

        createRequest = new CreateEventRequest(
                "Test Event",
                "Test Description",
                date,
                start,
                end,
                1L
        );
    }

    @Test
    void testAddEvent() {
        when(eventService.addEvent(
                anyString(),
                anyString(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class),
                anyLong()
        )).thenReturn(Mono.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.addEvent(createRequest))
                .expectNextMatches(dto ->
                        dto != null &&
                                dto.id().equals(1L) &&
                                dto.name().equals("Test Event") &&
                                dto.description().equals("Test Description")
                )
                .verifyComplete();

        verify(eventService).addEvent(
                eq(createRequest.name()),
                eq(createRequest.description()),
                eq(createRequest.date()),
                eq(createRequest.startTime()),
                eq(createRequest.endTime()),
                eq(createRequest.ownerId())
        );
        verify(eventMapper).toEventDto(testEvent);
    }

    @Test
    void testGetAllEvents() {
        Event event2 = new Event();
        event2.setId(2L);
        event2.setName("Event 2");

        EventDto dto2 = new EventDto(
                2L,
                "Event 2",
                null,
                null,
                null,
                null,
                null
        );

        when(eventService.getAllEvents()).thenReturn(Flux.just(testEvent, event2));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);
        when(eventMapper.toEventDto(event2)).thenReturn(dto2);

        StepVerifier.create(eventController.getAllEvents())
                .expectNext(testEventDto)
                .expectNext(dto2)
                .verifyComplete();

        verify(eventService).getAllEvents();
        verify(eventMapper).toEventDto(testEvent);
        verify(eventMapper).toEventDto(event2);
    }

    @Test
    void testGetEventById() {
        when(eventService.getEventById(1L)).thenReturn(Mono.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.getEventById(1L))
                .expectNextMatches(dto ->
                        dto != null &&
                                dto.id().equals(1L) &&
                                dto.name().equals("Test Event")
                )
                .verifyComplete();

        verify(eventService).getEventById(1L);
        verify(eventMapper).toEventDto(testEvent);
    }

    @Test
    void testGetUserEvents() {
        when(eventService.getUserEventsById(eq(1L), any()))
                .thenReturn(Flux.just(testEvent));

        StepVerifier.create(eventController.getUserEvents(1L, 0, 10))
                .expectNext(testEvent)
                .verifyComplete();

        verify(eventService).getUserEventsById(eq(1L), any());
    }

    @Test
    void testDeleteEventById() {
        when(eventService.deleteEventById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(eventController.deleteEventById(1L))
                .verifyComplete();

        verify(eventService).deleteEventById(1L);
    }

    @Test
    void testGetBusyEventsForUsersBetweenDates() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(7);

        when(eventService.getBusyEventsForUsersBetweenDates(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Flux.just(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        StepVerifier.create(eventController.getBusyEventsForUsersBetweenDates(
                        List.of(1L, 2L),
                        start,
                        end
                ))
                .expectNext(testEventDto)
                .verifyComplete();

        verify(eventService).getBusyEventsForUsersBetweenDates(eq(List.of(1L, 2L)), eq(start), eq(end));
        verify(eventMapper).toEventDto(testEvent);
    }
}
