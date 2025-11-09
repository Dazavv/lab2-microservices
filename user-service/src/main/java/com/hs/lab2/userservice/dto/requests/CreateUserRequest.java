package com.hs.lab2.userservice.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        String name,
        String surname
) {}
