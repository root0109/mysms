package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.enrollment (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EnrollmentEntity extends BaseEntity {

    public static final String TABLE = "lms.enrollment";

    private UUID tenantId;
    private UUID courseId;
    private UUID userId;
    private String memberType;
    private String memberRole;
    private BigDecimal progressPercent;
    private UUID currentLessonId;
    private UUID sourceCohortId;
    private UUID paymentId;
    private Boolean purchasedCertificate;
    private UUID certificateId;
}
