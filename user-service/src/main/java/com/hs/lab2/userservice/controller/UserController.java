package com.hs.lab2.userservice.controller;
import com.hs.lab2.userservice.dto.UserDto;
import com.hs.lab2.userservice.entity.User;
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
            @PageableDefault(page = 0, size = 25, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        int maxSize = Math.min(pageable.getPageSize(), 100);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());

        return userService.getAllUsers(safePageable)
                .map(users -> users.map(userMapper::toUserDto))
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
    public ResponseEntity<Page<UserDto>> searchByUsername(
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(page = 0, size = 25, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        int maxSize = Math.min(pageable.getPageSize(), 100);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());

        Page<User> users = userService.searchByUsername(q, safePageable);
        Page<UserDto> dtoPage = users.map(userMapper::toUserDto);
        return ResponseEntity.ok(dtoPage);
    }

    @DeleteMapping(path = "/id/{id}")
    public Mono<Void> deleteUserById(@PathVariable @Min(1) Long id) {
        return userService.deleteUserById(id)
                .then(Mono.just(ResponseEntity.ok().build()).then());
    }
}
