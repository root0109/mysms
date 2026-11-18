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

/** A row of lms.gradebook_entry. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GradebookEntryEntity extends BaseEntity {

    public static final String TABLE = "lms.gradebook_entry";

    private UUID tenantId;
    private BigDecimal pointsPossible;
    private UUID cohortId;
    private UUID contentItemId;
    private UUID userId;
    private UUID groupId;
    private UUID quizSubmissionId;
    private UUID forumTopicId;
    private BigDecimal points;
    private BigDecimal autoPoints;
    private BigDecimal manualPoints;
    private BigDecimal peerPoints;
    private String feedback;
    private String teacherComment;
    private String status;
    private UUID gradedBy;
    private OffsetDateTime gradedAt;
    private Boolean isPublished;
    private OffsetDateTime publishedAt;
}
