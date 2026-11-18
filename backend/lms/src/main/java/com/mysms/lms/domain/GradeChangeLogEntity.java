package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.grade_change_log. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GradeChangeLogEntity extends BaseEntity {

    public static final String TABLE = "lms.grade_change_log";

    private UUID tenantId;
    private UUID gradebookEntryId;
    private BigDecimal points;
    private BigDecimal autoPoints;
    private BigDecimal manualPoints;
    private String comment;
    private Boolean wasPublished;
    private OffsetDateTime publishedAt;
    private UUID changedBy;
    private OffsetDateTime changedAt;
    private UUID studentUserId;
}
