package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.UserClient;
import com.hs.lab2.groupeventservice.dto.responses.UserDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.exceptions.UserNotFoundException;
import com.hs.lab2.groupeventservice.exceptions.UserServiceUnavailableException;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupEventService {
    private final GroupEventRepository groupEventRepository;
    private final UserClient userClient;

    public Flux<GroupEvent> getAllGroupEvents() {
        return Mono.fromCallable(groupEventRepository::findAllWithParticipants).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable);
    }

    @Transactional
    public Mono<GroupEvent> addGroupEvent(String name,
                                          String description,
                                          List<Long> participantsIds,
                                          Long ownerId
    ) {
        return getUserByIdWithCircuitBreaker(ownerId)
                .flatMap(owner ->
                        Flux.fromIterable(participantsIds)
                                .flatMap(this::getUserByIdWithCircuitBreaker
                                )
                                .collectList()
                                .flatMap(participants ->
                                        Mono.fromCallable(() -> {
                                            GroupEvent groupEvent = GroupEvent.builder()
                                                    .name(name)
                                                    .description(description)
                                                    .participantIds(participantsIds)
                                                    .ownerId(ownerId)
                                                    .status(GroupEventStatus.PENDING)
                                                    .build();

                                            return groupEventRepository.save(groupEvent);
                                        }).subscribeOn(Schedulers.boundedElastic())
                                )
                );
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    public Mono<UserDto> getUserByIdWithCircuitBreaker(Long ownerId) {
        return userClient.getUserById(ownerId)
                .onErrorResume(FeignException.NotFound.class, e -> Mono.error(new UserNotFoundException("User with id = " + ownerId + " not found")))
                .onErrorResume(FeignException.class, e -> Mono.error(new RuntimeException("User-service error: " + e.status() + " " + e.getMessage())));
    }

    public Mono<GroupEvent> getGroupEventById(Long id) {
        return Mono.fromCallable(() -> groupEventRepository.findByIdWithParticipants(id).orElseThrow(() -> new EventNotFoundException("Group event with id = " + id + " not found")))

                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteGroupEventById(Long id) {
        return Mono.fromRunnable(() -> {
            if (!groupEventRepository.existsById(id))
                throw new EventNotFoundException("Group event with id = " + id + " not found");
            groupEventRepository.deleteById(id);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<UserDto> userFallback(Long ownerId, Throwable t) {
        return Mono.error(new UserServiceUnavailableException("User-service unavailable, try later"));
    }
}

