package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.timetable_entry. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TimetableEntryEntity extends BaseEntity {

    public static final String TABLE = "lms.timetable_entry";

    private UUID tenantId;
    private UUID cohortId;
    private UUID timetableTemplateId;
    private UUID lessonId;
    private UUID quizId;
    private UUID assignmentId;
    private UUID exerciseId;
    private UUID liveClassId;
    private LocalDate entryDate;
    private Integer dayNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private Boolean isMilestone;
}
