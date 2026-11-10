package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum_read_state (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumReadStateEntity extends BaseEntity {

    public static final String TABLE = "lms.forum_read_state";

    private UUID tenantId;
    private UUID userId;
    private UUID forumTopicId;
    private UUID forumPostId;
    private OffsetDateTime readAt;
    private Boolean isFlagged;
}
