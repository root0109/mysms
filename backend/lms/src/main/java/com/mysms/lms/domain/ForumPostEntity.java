package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum_post. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumPostEntity extends BaseEntity {

    public static final String TABLE = "lms.forum_post";

    private UUID tenantId;
    private UUID forumTopicId;
    private UUID parentPostId;
    private String title;
    private String body;
    private UUID authorUserId;
    private OffsetDateTime postedAt;
    private Boolean isDraft;
    private Boolean isTeacherPost;
    private Integer substantiveFlag;
    private Boolean isHidden;
    private Boolean isHiddenByParent;
    private Boolean isFlagged;
    private Boolean showSignature;
    private Boolean usesInlineAuthoring;
    private Boolean isAbusive;
    private Boolean abuseReported;
    private Integer abuseReportCount;
    private UUID abuseConfirmedBy;
    private OffsetDateTime abuseConfirmedAt;
    private Integer likeCount;
}
