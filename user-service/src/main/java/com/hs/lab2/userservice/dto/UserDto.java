package com.hs.lab2.userservice.dto;

import java.util.List;

public record UserDto (
        Long id,
        String username,
        String name,
        String surname,
        List<String> eventNames
) {}
