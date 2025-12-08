package com.hs.lab2.eventservice.service;

import com.hs.lab2.eventservice.dto.responses.UserDto;
import com.hs.lab2.eventservice.entity.Event;
import com.hs.lab2.eventservice.exceptions.EventConflictException;
import com.hs.lab2.eventservice.exceptions.EventNotFoundException;
import com.hs.lab2.eventservice.exceptions.UserNotFoundException;
import com.hs.lab2.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private UserDto testUser;
    private LocalDate tomorrow;
    private LocalTime startTime;
    private LocalTime endTime;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDate.now().plusDays(1);
        startTime = LocalTime.of(10, 0);
        endTime = LocalTime.of(11, 0);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(tomorrow);
        testEvent.setStartTime(startTime);
        testEvent.setEndTime(endTime);
        testEvent.setOwnerId(1L);

        testUser = new UserDto(1L, "testuser", "Test", "User", null);
    }

    @Test
    void testGetAllEvents() {
        Event event2 = new Event();
        event2.setId(2L);

        when(eventRepository.findAll()).thenReturn(Flux.just(testEvent, event2));

        StepVerifier.create(eventService.getAllEvents())
                .expectNext(testEvent)
                .expectNext(event2)
                .verifyComplete();

        verify(eventRepository).findAll();
    }

    @Test
    void testAddEvent_Success() {
        when(userClientService.getUserById(1L)).thenReturn(Mono.just(testUser));
        when(eventRepository.existsByOwnerAndDateAndTimeOverlap(anyLong(), any(), any(), any()))
                .thenReturn(Mono.just(false));
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(testEvent));

        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", tomorrow, startTime, endTime, 1L))
                .expectNextMatches(event -> event.getName().equals("Test Event"))
                .verifyComplete();

        verify(userClientService).getUserById(1L);
        verify(eventRepository).existsByOwnerAndDateAndTimeOverlap(1L, tomorrow, startTime, endTime);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testAddEvent_InvalidTime_EndBeforeStart() {
        LocalTime invalidEndTime = LocalTime.of(9, 0);

        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", tomorrow, startTime, invalidEndTime, 1L))
                .expectError(EventConflictException.class)
                .verify();

        verifyNoInteractions(userClientService);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testAddEvent_InvalidTime_PastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", pastDate, startTime, endTime, 1L))
                .expectError(EventConflictException.class)
                .verify();

        verifyNoInteractions(userClientService);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testAddEvent_InvalidTime_EqualTimes() {
        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", tomorrow, startTime, startTime, 1L))
                .expectError(EventConflictException.class)
                .verify();

        verifyNoInteractions(userClientService);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testAddEvent_TimeConflict() {
        when(userClientService.getUserById(1L)).thenReturn(Mono.just(testUser));
        when(eventRepository.existsByOwnerAndDateAndTimeOverlap(anyLong(), any(), any(), any()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", tomorrow, startTime, endTime, 1L))
                .expectError(EventConflictException.class)
                .verify();

        verify(userClientService).getUserById(1L);
        verify(eventRepository).existsByOwnerAndDateAndTimeOverlap(1L, tomorrow, startTime, endTime);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testAddEvent_UserNotFound() {
        when(userClientService.getUserById(1L))
                .thenReturn(Mono.error(new UserNotFoundException("User not found")));

        StepVerifier.create(eventService.addEvent(
                        "Test Event", "Description", tomorrow, startTime, endTime, 1L))
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userClientService).getUserById(1L);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testGetEventById_Success() {
        when(eventRepository.findById(1L)).thenReturn(Mono.just(testEvent));

        StepVerifier.create(eventService.getEventById(1L))
                .expectNext(testEvent)
                .verifyComplete();

        verify(eventRepository).findById(1L);
    }

    @Test
    void testGetEventById_NotFound() {
        when(eventRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.getEventById(1L))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(eventRepository).findById(1L);
    }

    @Test
    void testDeleteEventById_Success() {
        when(eventRepository.findById(1L)).thenReturn(Mono.just(testEvent));
        when(eventRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.deleteEventById(1L))
                .verifyComplete();

        verify(eventRepository).findById(1L);
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void testDeleteEventById_NotFound() {
        when(eventRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(eventService.deleteEventById(1L))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(eventRepository).findById(1L);
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetBusyEventsForUsersBetweenDates() {
        Event event2 = new Event();
        event2.setId(2L);

        when(eventRepository.findBusyEventsForUsersBetweenDates(anyList(), any(), any()))
                .thenReturn(Flux.just(testEvent, event2));

        StepVerifier.create(eventService.getBusyEventsForUsersBetweenDates(
                        List.of(1L, 2L), tomorrow, tomorrow.plusDays(7)))
                .expectNext(testEvent)
                .expectNext(event2)
                .verifyComplete();

        verify(eventRepository).findBusyEventsForUsersBetweenDates(
                eq(List.of(1L, 2L)), eq(tomorrow), eq(tomorrow.plusDays(7)));
    }

    @Test
    void testGetUserEventsById() {
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.findByOwnerIdPaged(eq(1L), eq(10L), eq(0L)))
                .thenReturn(Flux.just(testEvent));

        StepVerifier.create(eventService.getUserEventsById(1L, pageable))
                .expectNext(testEvent)
                .verifyComplete();

        verify(eventRepository).findByOwnerIdPaged(1L, 10L, 0L);
    }

    @Test
    void testGetUserEventsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.countByOwnerId(1L)).thenReturn(Mono.just(1L));
        when(eventRepository.findByOwnerIdPaged(eq(1L), eq(10L), eq(0L)))
                .thenReturn(Flux.just(testEvent));

        StepVerifier.create(eventService.getUserEventsPage(1L, pageable))
                .expectNextMatches(page ->
                        page.getTotalElements() == 1 &&
                                page.getContent().size() == 1 &&
                                page.getContent().get(0).getId().equals(1L)
                )
                .verifyComplete();

        verify(eventRepository).countByOwnerId(1L);
        verify(eventRepository).findByOwnerIdPaged(1L, 10L, 0L);
    }
}
