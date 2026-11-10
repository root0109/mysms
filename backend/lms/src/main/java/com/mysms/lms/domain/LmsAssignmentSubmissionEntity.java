package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.assignment_submission (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LmsAssignmentSubmissionEntity extends BaseEntity {

    public static final String TABLE = "lms.assignment_submission";

    private UUID tenantId;
    private UUID assignmentId;
    private UUID userId;
    private UUID lessonId;
    private UUID courseId;
    private String submissionType;
    private String questionSnapshot;
    private String answer;
    private UUID attachmentFileId;
    private String status;
    private UUID evaluatorUserId;
    private String evaluatorComments;
    private UUID contentItemId;
    private UUID groupId;
    private String title;
    private OffsetDateTime submittedAt;
    private Boolean isFinal;
    private OffsetDateTime finalizedAt;
}
