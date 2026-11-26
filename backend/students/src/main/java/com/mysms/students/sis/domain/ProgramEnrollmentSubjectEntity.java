package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.program_enrollment_subject. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProgramEnrollmentSubjectEntity extends BaseEntity {

    public static final String TABLE = "sis.program_enrollment_subject";

    private UUID tenantId;
    private UUID programEnrollmentId;
    private UUID subjectId;
}
