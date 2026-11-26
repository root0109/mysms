package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.evaluator. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EvaluatorEntity extends BaseEntity {

    public static final String TABLE = "lms.evaluator";

    private UUID tenantId;
    private UUID userId;
    private LocalDate unavailableFrom;
    private LocalDate unavailableTo;
    private String calendarProvider;
    private String calendarState;
    private OffsetDateTime calendarRequestedAt;
    private OffsetDateTime calendarConnectedAt;
}
