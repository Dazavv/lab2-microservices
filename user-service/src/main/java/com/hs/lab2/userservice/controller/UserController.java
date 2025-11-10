package com.hs.lab2.userservice.controller;

import com.hs.lab2.userservice.dto.UserDto;
import com.hs.lab2.userservice.mapper.UserMapper;
import com.hs.lab2.userservice.requests.CreateUserRequest;
import com.hs.lab2.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public Mono<ResponseEntity<UserDto>> addUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.addUser(
                        request.username(),
                        request.name(),
                        request.surname()
                )
                .map(userMapper::toUserDto)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @GetMapping
    public Mono<ResponseEntity<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort s = (sort == null || sort.isEmpty())
                ? Sort.by("username").ascending()
                : Sort.by(
                sort.stream().map(sv -> {
                    var parts = sv.split(",", 2);
                    return new Sort.Order(
                            parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                                    ? Sort.Direction.DESC : Sort.Direction.ASC,
                            parts[0]
                    );
                }).toList()
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize, s);

        return userService.getAllUsers(pageable)
                .map(p -> p.map(userMapper::toUserDto))
                .map(ResponseEntity::ok);
    }


    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<UserDto>> getUserById(@PathVariable @Min(1) Long id) {
        return userService.getUserById(id)
                .map(userMapper::toUserDto)
                .map(ResponseEntity::ok);
    }

    @GetMapping(path = "/by-username/{username}")
    public Mono<ResponseEntity<UserDto>> getUserByUsername(@PathVariable @NotBlank String username) {
        return userService.getUserByUsername(username)
                .map(userMapper::toUserDto)
                .map(ResponseEntity::ok);
    }

    @GetMapping(path = "/search")
    public Mono<ResponseEntity<Page<UserDto>>> searchByUsername(
            @RequestParam(name = "q", defaultValue = "") String q,
            @PageableDefault(size = 25, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        int maxSize = Math.min(pageable.getPageSize(), 100);
        Pageable safe = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());

        return userService.searchByUsername(q, safe)
                .map(page -> page.map(userMapper::toUserDto))
                .map(ResponseEntity::ok);
    }


    @DeleteMapping(path = "/id/{id}")
    public Mono<Void> deleteUserById(@PathVariable @Min(1) Long id) {
        return userService.deleteUserById(id)
                .then(Mono.just(ResponseEntity.ok().build()).then());
    }
}
