package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notice_version (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NoticeVersionEntity extends BaseEntity {

    public static final String TABLE = "comms.notice_version";

    private UUID tenantId;
    private UUID noticeId;
    private String body;
    private OffsetDateTime publishAt;
    private OffsetDateTime expireAt;
    private UUID discussionTopicId;
    private Boolean isClosed;
    private Boolean isDeactivated;
    private String authorComment;
    private OffsetDateTime versionedAt;
    private OffsetDateTime originalCreatedAt;
}
