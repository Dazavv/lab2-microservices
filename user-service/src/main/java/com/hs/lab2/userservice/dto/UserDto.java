package com.hs.lab1.dto;

import java.util.List;

public record UserDto (
        Long id,
        String username,
        String name,
        String surname,
        List<String> eventNames
) {}
