package com.hs.lab2.groupeventservice.client;

import com.hs.lab2.groupeventservice.dto.responses.EventDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

@ReactiveFeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/api/v1/event/busy")
    Flux<EventDto> getBusyEventsForUsersBetweenDates(
            @RequestParam List<Long> userIds,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    );
}
