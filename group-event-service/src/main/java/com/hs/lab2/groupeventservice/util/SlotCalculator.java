package com.hs.lab2.groupeventservice.util;



import com.hs.lab2.groupeventservice.dto.responses.TimeInterval;
import com.hs.lab2.groupeventservice.dto.responses.RecommendTimeSlotDto;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public final class SlotCalculator {

    private SlotCalculator() {
    }

    public static List<RecommendTimeSlotDto> findCommonFreeSlots(
            LocalDate periodStart,
            LocalDate periodEnd,
            List<TimeInterval> busyIntervals,
            Duration minDuration
    ) {
        if (periodEnd.isBefore(periodStart)) throw new IllegalArgumentException("periodEnd < periodStart");

        List<RecommendTimeSlotDto> result = new ArrayList<>();
        Map<LocalDate, List<TimeInterval>> byDate = Optional.ofNullable(busyIntervals).orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(TimeInterval::date));

        for (LocalDate d = periodStart; !d.isAfter(periodEnd); d = d.plusDays(1)) {
            List<TimeInterval> dayBusy = merge(byDate.getOrDefault(d, List.of()));
            LocalTime cursor = LocalTime.MIN;

            for (TimeInterval b : dayBusy) {
                if (b.start().isAfter(cursor)) {
                    Duration gap = Duration.between(cursor, b.start());
                    if (!gap.minus(minDuration).isNegative()) {
                        result.add(new RecommendTimeSlotDto(d, cursor, b.start()));
                    }
                }
                if (b.end().isAfter(cursor)) cursor = b.end();
            }
            if (cursor.isBefore(LocalTime.MAX)) {
                Duration gap = Duration.between(cursor, LocalTime.MAX);
                if (!gap.minus(minDuration).isNegative()) {
                    result.add(new RecommendTimeSlotDto(d, cursor, LocalTime.MAX));
                }
            }
        }
        return result;
    }

    private static List<TimeInterval> merge(List<TimeInterval> intervals) {
        if (intervals.isEmpty()) return List.of();
        List<TimeInterval> sorted = intervals.stream()
                .sorted(Comparator.comparing(TimeInterval::start).thenComparing(TimeInterval::end))
                .toList();

        List<TimeInterval> merged = new ArrayList<>();
        TimeInterval cur = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            TimeInterval next = sorted.get(i);
            if (!next.start().isAfter(cur.end())) {
                cur = new TimeInterval(cur.date(), cur.start(),
                        next.end().isAfter(cur.end()) ? next.end() : cur.end());
            } else {
                merged.add(cur);
                cur = next;
            }
        }
        merged.add(cur);
        return merged;
    }
}