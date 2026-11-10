package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notice (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NoticeEntity extends BaseEntity {

    public static final String TABLE = "comms.notice";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String audience;
    private UUID classSectionId;
    private UUID backgroundId;
    private String title;
    private String body;
    private UUID attachmentFileId;
    private OffsetDateTime publishAt;
    private OffsetDateTime expireAt;
    private Boolean isPublished;
    private UUID cohortId;
    private Boolean showInCalendar;
    private Boolean allowReplies;
    private UUID discussionTopicId;
    private Boolean isClosed;
    private Boolean isDeactivated;
    private String authorComment;
}
