package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort_session (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortSessionEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort_session";

    private UUID tenantId;
    private UUID cohortId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String description;
    private String venue;
    private Boolean isCancelled;
}
