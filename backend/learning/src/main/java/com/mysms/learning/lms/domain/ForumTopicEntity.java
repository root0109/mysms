package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum_topic. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumTopicEntity extends BaseEntity {

    public static final String TABLE = "lms.forum_topic";

    private UUID tenantId;
    private UUID forumSectionId;
    private String title;
    private String description;
    private String topicType;
    private OffsetDateTime opensAt;
    private OffsetDateTime closesAt;
    private UUID contentItemId;
    private UUID discussionPromptId;
    private String linkedEntityType;
    private UUID linkedEntityId;
    private UUID individualUserId;
    private Boolean isGraded;
    private BigDecimal pointsPossible;
    private BigDecimal gradeWeightPercent;
    private OffsetDateTime gradingDueAt;
    private Boolean isGradingCompleted;
    private Boolean isHidden;
    private Boolean isTeacherTopic;
    private Boolean isSystemGenerated;
    private UUID lastPostId;
}
