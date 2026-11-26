package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.exam. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExamEntity extends BaseEntity {

    public static final String TABLE = "assessment.exam";

    private UUID tenantId;
    private UUID examSessionId;
    private UUID subjectId;
    private UUID classSectionId;
    private String code;
    private String name;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private BigDecimal totalMarks;
    private BigDecimal passMarks;
    private String status;
    private Boolean resultsEntered;
    private String notes;
}
