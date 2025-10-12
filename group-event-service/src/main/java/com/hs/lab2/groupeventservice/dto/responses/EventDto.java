package com.hs.lab2.groupeventservice.dto.responses;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventDto(
        Long id,
        String name,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long ownerId
) {}
