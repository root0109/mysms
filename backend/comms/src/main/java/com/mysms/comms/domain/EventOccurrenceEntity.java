package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.event_occurrence. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EventOccurrenceEntity extends BaseEntity {

    public static final String TABLE = "comms.event_occurrence";

    private UUID tenantId;
    private UUID eventId;
    private UUID userId;
    private UUID cohortId;
    private String title;
    private String color;
    private String description;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Boolean isCancelled;
    private String responseStatus;
    private String importance;
}
