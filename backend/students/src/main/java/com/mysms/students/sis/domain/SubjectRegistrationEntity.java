package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.subject_registration. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SubjectRegistrationEntity extends BaseEntity {

    public static final String TABLE = "sis.subject_registration";

    private UUID tenantId;
    private UUID academicYearId;
    private UUID academicTermId;
    private UUID programEnrollmentId;
    private UUID studentId;
    private UUID courseId;
    private UUID batchId;
    private String referenceNo;
    private String status;
    private BigDecimal minUnitLoad;
    private BigDecimal maxUnitLoad;
    private Boolean readByStudent;
    private Boolean readByGuardian;
}
