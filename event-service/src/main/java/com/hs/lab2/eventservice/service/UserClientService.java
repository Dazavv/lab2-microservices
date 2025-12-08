package com.hs.lab2.eventservice.service;

import com.hs.lab2.eventservice.client.UserClient;
import com.hs.lab2.eventservice.dto.responses.UserDto;
import com.hs.lab2.eventservice.exceptions.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserClientService {
    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    public Mono<UserDto> getUserById(Long ownerId) {
        return userClient.getUserById(ownerId);
    }

    public Mono<UserDto> userFallback(Long ownerId, Throwable t) {
        return Mono.error(new UserServiceUnavailableException("User-service unavailable, try later"));
    }
}
