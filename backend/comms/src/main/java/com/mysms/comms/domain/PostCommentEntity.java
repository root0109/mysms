package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.post_comment (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PostCommentEntity extends BaseEntity {

    public static final String TABLE = "comms.post_comment";

    private UUID tenantId;
    private UUID postId;
    private UUID parentCommentId;
    private UUID authorUserId;
    private String body;
    private UUID attachmentFileId;
    private Boolean isActive;
}
