package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.reaction: one user's reaction to a post or to a comment (exactly one of the two). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReactionEntity extends BaseEntity {

    public static final String TABLE = "comms.reaction";

    private UUID tenantId;
    private UUID postId;
    private UUID commentId;
    private UUID userId;
    private String reaction;
    private Boolean isSaved;
    private Boolean isActive;
}
