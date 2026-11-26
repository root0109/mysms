package com.mysms.platform.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.academic_term. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AcademicTermEntity extends BaseEntity {

    public static final String TABLE = "org.academic_term";

    private UUID tenantId;
    private UUID academicYearId;
    private UUID parentTermId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sequence;
    private String code;
    private Boolean isActive;
}
