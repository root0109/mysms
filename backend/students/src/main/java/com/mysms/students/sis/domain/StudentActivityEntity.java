package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.student_activity. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentActivityEntity extends BaseEntity {

    public static final String TABLE = "sis.student_activity";

    private UUID tenantId;
    private UUID studentId;
    private UUID staffId;
    private UUID activityTypeId;
    private LocalDate activityDate;
    private String description;
}
