package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.content_item. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContentItemEntity extends BaseEntity {

    public static final String TABLE = "lms.content_item";

    private UUID tenantId;
    private UUID exerciseId;
    private UUID chapterId;
    private UUID courseId;
    private UUID cohortId;
    private String itemKind;
    private UUID lessonId;
    private UUID fileId;
    private UUID assignmentId;
    private UUID quizId;
    private UUID discussionPromptId;
    private UUID participationActivityId;
    private UUID webResourceId;
    private UUID ebookId;
    private UUID externalToolResourceId;
    private UUID liveClassId;
    private String linkUrl;
    private String title;
    private Integer sortOrder;
    private Boolean isActive;
    private Boolean isAlwaysVisible;
    private OffsetDateTime visibleFrom;
    private OffsetDateTime visibleUntil;
    private Boolean visibleUntilCohortEnd;
    private OffsetDateTime attemptFrom;
    private OffsetDateTime dueAt;
    private Boolean attemptUntilCohortEnd;
    private Integer visibleStartDay;
    private LocalTime visibleStartTime;
    private Integer visibleEndDay;
    private LocalTime visibleEndTime;
    private Integer attemptStartDay;
    private LocalTime attemptStartTime;
    private Integer attemptEndDay;
    private LocalTime attemptEndTime;
    private Boolean lateSubmissionAllowed;
    private Integer lateSubmissionDays;
    private Boolean isGraded;
    private BigDecimal points;
    private BigDecimal weightPercent;
    private UUID gradingCategoryId;
    private UUID rubricId;
    private Boolean plagiarismCheckRequired;
    private Boolean showInCalendar;
    private Boolean allowDelete;
    private Boolean allowEditDetails;
    private Boolean allowEditDates;
    private Boolean allowEditGrading;
    private UUID copiedFromId;
    private Boolean isOriginal;
    private Long viewCount;
    private Long likeCount;
    private BigDecimal ratingTotal;
    private Integer ratingCount;
}
