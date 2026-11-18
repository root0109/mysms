package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.content_view. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContentViewEntity extends BaseEntity {

    public static final String TABLE = "comms.content_view";

    private UUID tenantId;
    private UUID institutionId;
    private UUID studentId;
    private UUID viewerUserId;
    private String contentType;
    private UUID contentId;
    private OffsetDateTime viewedAt;
}
