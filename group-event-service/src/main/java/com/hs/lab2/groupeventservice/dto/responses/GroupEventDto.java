package com.hs.lab2.groupeventservice.dto.responses;

import com.hs.lab2.groupeventservice.enums.GroupEventStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GroupEventDto(
        Long id,
        String name,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        List<Long> participantIds,
        Long ownerId,
        GroupEventStatus status
) {
}
