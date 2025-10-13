package com.hs.lab2.groupeventservice.client;

import com.hs.lab2.groupeventservice.dto.responses.UserDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/user/{id}")
    Mono<UserDto> getUserById(@PathVariable("id") Long id);
}
