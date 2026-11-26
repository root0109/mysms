package com.mysms.academics.timetable.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of timetable.period. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PeriodEntity extends BaseEntity {

    public static final String TABLE = "timetable.period";

    private UUID tenantId;
    private UUID institutionId;
    private String name;
    private LocalTime startTime;
    private BigDecimal durationMinutes;
    private Integer sequence;
}
