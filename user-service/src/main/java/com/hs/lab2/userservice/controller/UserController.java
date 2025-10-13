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

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserDto> addUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.addUser(request.username(), request.name(), request.surname());
        UserDto userDto = userMapper.toUserDto(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userDto);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(userMapper.toUserDtoList(users));
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable @Min(1) Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toUserDto(user));
    }

    @GetMapping(path = "/by-username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable @NotBlank String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(userMapper.toUserDto(user));
    }

    @DeleteMapping(path = "/id/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable @Min(1) Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
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

}
