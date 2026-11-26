package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum_post_vote. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumPostVoteEntity extends BaseEntity {

    public static final String TABLE = "lms.forum_post_vote";

    private UUID tenantId;
    private UUID forumPostId;
    private UUID userId;
    private Boolean isLike;
    private OffsetDateTime votedAt;
}
