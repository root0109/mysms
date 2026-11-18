package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.certificate_evaluation. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CertificateEvaluationEntity extends BaseEntity {

    public static final String TABLE = "lms.certificate_evaluation";

    private UUID tenantId;
    private UUID courseId;
    private UUID userId;
    private UUID cohortId;
    private UUID certificateRequestId;
    private UUID evaluatorUserId;
    private LocalDate evaluationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal rating;
    private String summary;
    private String status;
}
