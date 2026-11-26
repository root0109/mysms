package com.mysms.academics.timetable.business;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Expands a weekly pattern into the dated sessions it means between two dates: each slot meets on its ISO weekday at
 * its period's local start time in the institution's time zone, for the period's length. Days that do not exist in
 * the zone (a clock change) move to the next valid instant, as {@link java.time.ZonedDateTime} resolves them.
 */
final class WeeklyPattern {

    /** One weekly slot with its period resolved. */
    record Slot(int weekday, UUID periodId, LocalTime startTime, Duration length, UUID subjectId, UUID staffId,
            UUID roomId) {
    }

    /** One dated meeting of a slot, in UTC. */
    record Occurrence(Slot slot, LocalDate date, OffsetDateTime startAt, OffsetDateTime endAt) {
    }

    private WeeklyPattern() {
    }

    /** Every meeting of the slots from {@code from} to {@code to} (inclusive), in start order. */
    static List<Occurrence> expand(List<Slot> slots, LocalDate from, LocalDate to, ZoneId zone) {
        List<Occurrence> occurrences = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int weekday = day.getDayOfWeek().getValue();
            for (Slot slot : slots) {
                if (slot.weekday() == weekday) {
                    OffsetDateTime start = day.atTime(slot.startTime()).atZone(zone).toOffsetDateTime()
                            .withOffsetSameInstant(ZoneOffset.UTC);
                    occurrences.add(new Occurrence(slot, day, start, start.plus(slot.length())));
                }
            }
        }
        occurrences.sort(Comparator.comparing(Occurrence::startAt));
        return occurrences;
    }

    /** How many meetings the slots have between the dates, without building them. */
    static long count(List<Integer> weekdays, LocalDate from, LocalDate to) {
        long count = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int weekday = day.getDayOfWeek().getValue();
            count += weekdays.stream().filter(w -> w == weekday).count();
        }
        return count;
    }
}
