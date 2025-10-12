package com.hs.lab2.groupeventservice.dto.responses;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeInterval(LocalDate date, LocalTime start, LocalTime end) {}