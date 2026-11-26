package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort_schedule_slot. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortScheduleSlotEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort_schedule_slot";

    private UUID tenantId;
    private UUID cohortId;
    private String slotType;
    private UUID cohortTeacherId;
    private String weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
}
