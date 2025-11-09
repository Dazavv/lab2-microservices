package com.hs.lab2.groupeventservice.service;

import com.hs.lab2.groupeventservice.client.EventClient;
import com.hs.lab2.groupeventservice.dto.responses.RecommendTimeSlotDto;
import com.hs.lab2.groupeventservice.dto.responses.TimeInterval;
import com.hs.lab2.groupeventservice.entity.GroupEvent;
import com.hs.lab2.groupeventservice.enums.GroupEventStatus;
import com.hs.lab2.groupeventservice.exceptions.EventNotFoundException;
import com.hs.lab2.groupeventservice.exceptions.EventServiceUnavailableException;
import com.hs.lab2.groupeventservice.exceptions.NoAvailableSlotsException;
import com.hs.lab2.groupeventservice.repository.GroupEventRepository;
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
    private final GroupEventRepository groupEventRepository;
    private final EventClient eventClient;
    private final GroupEventLoader loader;

    @Transactional
    public Flux<RecommendTimeSlotDto> recommendSlots(LocalDate periodStart, LocalDate periodEnd, Duration duration, Long groupEventId) {
        Mono<List<Long>> participantIdsMono = Mono.fromCallable(
                () -> loader.loadParticipantIds(groupEventId)).subscribeOn(Schedulers.boundedElastic());

        return participantIdsMono.flatMapMany(participantIds -> fetchBusyIntervals(participantIds, periodStart, periodEnd).collectList().flatMapMany(busyIntervals -> {
            var free = SlotCalculator.findCommonFreeSlots(periodStart, periodEnd, busyIntervals, duration);
            if (free.isEmpty()) return Flux.error(new NoAvailableSlotsException("No free slots available"));
            return Flux.fromIterable(free.stream().limit(5).toList());
        }));
    }


    @CircuitBreaker(name = "eventService", fallbackMethod = "fetchBusyIntervalsFallback")
    public Flux<TimeInterval> fetchBusyIntervals(List<Long> participantIds, LocalDate start, LocalDate end) {
        return eventClient.getBusyEventsForUsersBetweenDates(participantIds, start, end).map(e -> new TimeInterval(e.date(), e.startTime(), e.endTime()));
    }

    @SuppressWarnings("unused")
    public Flux<TimeInterval> fetchBusyIntervalsFallback(List<Long> participantIds, LocalDate start, LocalDate end, Throwable t) {
        return Flux.error(new EventServiceUnavailableException("Event-service unavailable, try later"));
    }

    @Transactional
    public Mono<GroupEvent> bookSlot(Long id, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return Mono.fromCallable(() -> groupEventRepository.findById(id)).subscribeOn(Schedulers.boundedElastic()).flatMap(optional -> optional.map(Mono::just).orElseGet(() -> Mono.error(new EventNotFoundException("GroupEvent not found: " + id)))).flatMap(groupEvent -> {
            groupEvent.setDate(date);
            groupEvent.setStartTime(startTime);
            groupEvent.setEndTime(endTime);
            groupEvent.setStatus(GroupEventStatus.CONFIRMED);

            return Mono.fromCallable(() -> groupEventRepository.save(groupEvent)).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
