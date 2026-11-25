package com.mysms.comms.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One item of a person's own calendar: an event, one occurrence of a recurring event, or a holiday. */
public record CalendarRow(String itemType, UUID id, String title, OffsetDateTime startAt, OffsetDateTime endAt,
        Boolean allDay, String color, UUID cohortId, Boolean isCancelled, Boolean isCompleted) {
}
