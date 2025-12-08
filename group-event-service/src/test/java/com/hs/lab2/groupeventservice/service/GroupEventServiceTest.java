package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.dto.responses.UserDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupEventServiceTest {

    @Mock
    private GroupEventRepository groupEventRepository;

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private GroupEventService groupEventService;

    private GroupEvent testGroupEvent;
    private UserDto testUser;
    private List<Long> participantIds;

    @BeforeEach
    void setUp() {
        participantIds = List.of(2L, 3L);

        testGroupEvent = GroupEvent.builder()
                .id(1L)
                .name("Test Group Event")
                .description("Test Description")
                .participantIds(participantIds)
                .ownerId(1L)
                .status(GroupEventStatus.PENDING)
                .build();

        testUser = new UserDto(1L, "testuser", "Test", "User", null);
    }

    @Test
    void testGetAllGroupEvents() {
        GroupEvent event2 = GroupEvent.builder()
                .id(2L)
                .name("Event 2")
                .build();

        when(groupEventRepository.findAllWithParticipants())
                .thenReturn(List.of(testGroupEvent, event2));

        StepVerifier.create(groupEventService.getAllGroupEvents())
                .expectNext(testGroupEvent)
                .expectNext(event2)
                .verifyComplete();

        verify(groupEventRepository).findAllWithParticipants();
    }

    @Test
    void testAddGroupEvent_Success() {
        UserDto owner = new UserDto(1L, "owner", "Owner", "Owner", null);

        when(userClientService.getUserById(1L)).thenReturn(Mono.just(owner));
        when(groupEventRepository.save(any(GroupEvent.class))).thenReturn(testGroupEvent);

        StepVerifier.create(groupEventService.addGroupEvent(
                        "Test Group Event", "Description", participantIds, 1L))
                .expectNextMatches(event ->
                        event.getName().equals("Test Group Event") &&
                                event.getStatus() == GroupEventStatus.PENDING &&
                                event.getOwnerId().equals(1L)
                )
                .verifyComplete();

        verify(userClientService).getUserById(1L);
        verify(groupEventRepository).save(any(GroupEvent.class));
    }

    @Test
    void testAddGroupEvent_OwnerNotFound() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(userClientService.getUserById(1L)).thenReturn(Mono.error(notFound));

        StepVerifier.create(groupEventService.addGroupEvent(
                        "Test Group Event", "Description", participantIds, 1L))
                .expectError(FeignException.NotFound.class)
                .verify();

        verify(userClientService).getUserById(1L);
        verify(groupEventRepository, never()).save(any(GroupEvent.class));
    }

    @Test
    void testGetGroupEventById_Success() {
        when(groupEventRepository.findByIdWithParticipants(1L))
                .thenReturn(Optional.of(testGroupEvent));

        StepVerifier.create(groupEventService.getGroupEventById(1L))
                .expectNext(testGroupEvent)
                .verifyComplete();

        verify(groupEventRepository).findByIdWithParticipants(1L);
    }

    @Test
    void testGetGroupEventById_NotFound() {
        when(groupEventRepository.findByIdWithParticipants(1L))
                .thenReturn(Optional.empty());

        StepVerifier.create(groupEventService.getGroupEventById(1L))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(groupEventRepository).findByIdWithParticipants(1L);
    }

    @Test
    void testDeleteGroupEventById_Success() {
        when(groupEventRepository.existsById(1L)).thenReturn(true);
        doNothing().when(groupEventRepository).deleteById(1L);

        StepVerifier.create(groupEventService.deleteGroupEventById(1L))
                .verifyComplete();

        verify(groupEventRepository).existsById(1L);
        verify(groupEventRepository).deleteById(1L);
    }

    @Test
    void testDeleteGroupEventById_NotFound() {
        when(groupEventRepository.existsById(1L)).thenReturn(false);

        StepVerifier.create(groupEventService.deleteGroupEventById(1L))
                .expectError(EventNotFoundException.class)
                .verify();

        verify(groupEventRepository).existsById(1L);
        verify(groupEventRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetUserByIdWithCircuitBreaker_Success() {
        when(userClientService.getUserById(1L)).thenReturn(Mono.just(testUser));

        StepVerifier.create(userClientService.getUserById(1L))
                .expectNext(testUser)
                .verifyComplete();

        verify(userClientService).getUserById(1L);
    }

    @Test
    void testGetUserByIdWithCircuitBreaker_NotFound() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(userClientService.getUserById(1L)).thenReturn(Mono.error(notFound));

        StepVerifier.create(userClientService.getUserById(1L))
                .expectError(FeignException.NotFound.class)
                .verify();

        verify(userClientService).getUserById(1L);
    }
}
