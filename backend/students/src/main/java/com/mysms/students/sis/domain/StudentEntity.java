package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.student. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentEntity extends BaseEntity {

    public static final String TABLE = "sis.student";

    private UUID tenantId;
    private UUID personId;
    private UUID userId;
    private UUID institutionId;
    private String admissionNumber;
    private LocalDate admissionDate;
    private LocalDate joiningDate;
    private UUID studentCategoryId;
    private String governmentStudentId;
    private String bonafideCertificateNumber;
    private String status;
    private LocalDate exitDate;
    private String exitReason;
}
