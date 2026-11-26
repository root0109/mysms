package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.program_enrollment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ProgramEnrollmentEntity extends BaseEntity {

    public static final String TABLE = "sis.program_enrollment";

    private UUID tenantId;
    private UUID studentId;
    private UUID courseId;
    private UUID batchId;
    private UUID academicYearId;
    private UUID academicTermId;
    private String rollNumber;
    private String status;
}
