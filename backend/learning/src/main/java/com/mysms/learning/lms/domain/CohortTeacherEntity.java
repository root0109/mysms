package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort_teacher. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortTeacherEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort_teacher";

    private UUID tenantId;
    private UUID cohortId;
    private UUID userId;
    private UUID roleId;
    private String roleLabel;
    private Boolean isActive;
    private Boolean hasOfficeHours;
    private OffsetDateTime removedAt;
}
