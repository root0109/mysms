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

/** A row of lms.plagiarism_report (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PlagiarismReportEntity extends BaseEntity {

    public static final String TABLE = "lms.plagiarism_report";

    private UUID tenantId;
    private UUID plagiarismAccountId;
    private UUID entityAttachmentId;
    private UUID fileId;
    private String itemKind;
    private String providerSubmissionRef;
    private String providerObjectRef;
    private OffsetDateTime submittedAt;
    private OffsetDateTime providerSubmittedAt;
    private String status;
    private String similarityScore;
    private String reportUrl;
    private BigDecimal tolerance;
    private String grademarkStatus;
    private String errorCode;
    private String errorText;
    private OffsetDateTime lastCheckedAt;
    private OffsetDateTime receivedAt;
}
