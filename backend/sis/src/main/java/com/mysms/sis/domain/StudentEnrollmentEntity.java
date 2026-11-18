package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.student_enrollment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentEnrollmentEntity extends BaseEntity {

    public static final String TABLE = "sis.student_enrollment";

    private UUID tenantId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private UUID previousEnrollmentId;
    private String changeReason;
    private UUID studentId;
    private UUID academicYearId;
    private UUID classSectionId;
    private String rollNumber;
    private String idCardNumber;
    private String boardRegistrationNumber;
    private String enrollmentStatus;
    private String resultStatus;
    private LocalDate enrolledOn;
    private LocalDate leftOn;
}
