package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.marksheet_line. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MarksheetLineEntity extends BaseEntity {

    public static final String TABLE = "assessment.marksheet_line";

    private UUID tenantId;
    private BigDecimal creditsAttempted;
    private BigDecimal creditsEarned;
    private BigDecimal gradePoints;
    private BigDecimal sgpa;
    private BigDecimal cgpa;
    private UUID marksheetId;
    private UUID studentId;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private String gradeLabel;
    private String resultStatus;
    private LocalDate generatedOn;
}
