package com.hs.lab2.groupeventservice.service;
import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.dto.responses.TimeInterval;
import com.hs.lab2.groupeventservice.exceptions.EventServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventClientService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "fetchBusyIntervalsFallback")
    public Flux<TimeInterval> getBusyEventsForUsersBetweenDates(List<Long> participantIds, String start, String end) {
        return eventClient.getBusyEventsForUsersBetweenDates(participantIds, start, end).map(e -> new TimeInterval(e.date(), e.startTime(), e.endTime()));
    }

    public Flux<TimeInterval> fetchBusyIntervalsFallback(List<Long> participantIds, LocalDate start, LocalDate end, Throwable t) {
        return Flux.error(new EventServiceUnavailableException("Event-service unavailable, try later"));
    }
}
