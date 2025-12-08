package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.dto.responses.GroupEventDto;
import com.hs.lab2.groupeventservice.dto.responses.RecommendTimeSlotDto;
import com.hs.lab2.groupeventservice.dto.responses.TimeInterval;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventServiceUnavailableException;
import com.hs.lab2.groupeventservice.exceptions.NoAvailableSlotsException;
import com.hs.lab2.groupeventservice.mapper.GroupEventMapper;
import com.hs.lab2.groupeventservice.util.SlotCalculator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final GroupEventService groupEventService;
    private final GroupEventMapper groupEventMapper;
    private final EventClient eventClient;
    private final GroupEventLoader loader;

    @CircuitBreaker(name = "eventService", fallbackMethod = "fetchBusyIntervalsFallback")
    @Transactional
    public Flux<RecommendTimeSlotDto> recommendSlots(LocalDate periodStart, LocalDate periodEnd, Duration duration, Long groupEventId) {
        Mono<List<Long>> participantIdsMono = Mono.fromCallable(
                () -> loader.loadParticipantIds(groupEventId)).subscribeOn(Schedulers.boundedElastic());

        return participantIdsMono.flatMapMany(participantIds -> fetchBusyIntervals(participantIds, periodStart.toString(), periodEnd.toString()).collectList().flatMapMany(busyIntervals -> {
            var free = SlotCalculator.findCommonFreeSlots(periodStart, periodEnd, busyIntervals, duration);
            if (free.isEmpty()) return Flux.error(new NoAvailableSlotsException("No free slots available"));
            return Flux.fromIterable(free.stream().limit(5).toList());
        }));
    }

    public Flux<TimeInterval> fetchBusyIntervals(List<Long> participantIds, String start, String end) {
        return eventClient.getBusyEventsForUsersBetweenDates(participantIds, start, end).map(e -> new TimeInterval(e.date(), e.startTime(), e.endTime()));
    }

    public Flux<TimeInterval> fetchBusyIntervalsFallback(LocalDate periodStart, LocalDate periodEnd, Duration duration, Long groupEventId, Throwable t) {
        return Flux.error(new EventServiceUnavailableException("Event-service unavailable, try later"));
    }

    @Transactional
    public Mono<GroupEventDto> bookSlot(Long id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return groupEventService.getGroupEventById(id)
                .flatMap(ge -> {
                    ge.setDate(date);
                    ge.setStartTime(startTime);
                    ge.setEndTime(endTime);
                    ge.setStatus(GroupEventStatus.CONFIRMED);
                    return groupEventService.saveGroupEvent(ge);
                })
                .map(groupEventMapper::toGroupEventDto);
    }
}
