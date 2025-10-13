package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.UserClient;
import com.hs.lab2.groupeventservice.dto.responses.UserDto;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.exceptions.UserNotFoundException;
import com.hs.lab2.groupeventservice.exceptions.UserServiceUnavailableException;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
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
        return Mono.fromCallable(groupEventRepository::findAllWithParticipants)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Transactional
    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    public Mono<GroupEvent> addGroupEvent(String name,
                                          String description,
                                          List<Long> participantsIds,
                                          Long ownerId
    ) {
        return userClient.getUserById(ownerId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Owner with id=" + ownerId + " not found")))
                .flatMap(owner ->
                        Flux.fromIterable(participantsIds)
                                .flatMap(id ->
                                        userClient.getUserById(id)
                                                .switchIfEmpty(Mono.error(new UserNotFoundException("Participant with id=" + id + " not found")))
                                )
                                .collectList()
                                .flatMap(participants ->
                                        Mono.fromCallable(() -> {
                                            GroupEvent groupEvent = new GroupEvent();
                                            groupEvent.setName(name);
                                            groupEvent.setDescription(description);
                                            groupEvent.setParticipantIds(participantsIds);
                                            groupEvent.setOwnerId(ownerId);
                                            groupEvent.setStatus(GroupEventStatus.PENDING);
                                            return groupEventRepository.save(groupEvent);
                                        }).subscribeOn(Schedulers.boundedElastic())
                                )
                );
    }

    public Mono<GroupEvent> getGroupEventById(Long id) {
        return Mono.fromCallable(() -> groupEventRepository.findByIdWithParticipants(id)
                        .orElseThrow(() -> new EventNotFoundException("Group event with id = " + id + " not found")))

                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteGroupEventById(Long id) {
        return Mono.fromRunnable(() -> {
                    if (!groupEventRepository.existsById(id))
                        throw new EventNotFoundException("Group event with id = " + id + " not found");
                    groupEventRepository.deleteById(id);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<GroupEvent> userFallback(String name,
                                          String description,
                                          List<Long> participantsIds,
                                          Long ownerId,
                                          Throwable t) {
        return Mono.error(new UserServiceUnavailableException("User-service unavailable, try later"));
    }

}

