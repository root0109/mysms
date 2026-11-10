package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.quiz_violation (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QuizViolationEntity extends BaseEntity {

    public static final String TABLE = "lms.quiz_violation";

    private UUID tenantId;
    private UUID quizSubmissionId;
    private String eventType;
    private String severity;
    private OffsetDateTime occurredAt;
}
