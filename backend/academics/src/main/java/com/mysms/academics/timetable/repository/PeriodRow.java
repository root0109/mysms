package com.mysms.academics.timetable.repository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/** A period as session generation reads it: where it belongs, when it starts and how long it lasts (null: unset). */
public record PeriodRow(UUID id, UUID institutionId, LocalTime startTime, BigDecimal durationMinutes) {
}
