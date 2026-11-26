package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.content_view_session. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContentViewSessionEntity extends BaseEntity {

    public static final String TABLE = "lms.content_view_session";

    private UUID tenantId;
    private UUID userId;
    private UUID contentItemId;
    private UUID fileId;
    private UUID cohortId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String location;
    private String sessionRef;
}
