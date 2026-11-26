package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.certificate. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CertificateEntity extends BaseEntity {

    public static final String TABLE = "lms.certificate";

    private UUID tenantId;
    private UUID userId;
    private UUID courseId;
    private UUID cohortId;
    private UUID evaluatorId;
    private String certificateNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String templateCode;
    private Boolean isPublished;
}
