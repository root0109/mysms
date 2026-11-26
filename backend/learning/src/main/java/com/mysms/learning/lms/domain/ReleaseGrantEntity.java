package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.release_grant. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReleaseGrantEntity extends BaseEntity {

    public static final String TABLE = "lms.release_grant";

    private UUID tenantId;
    private UUID contentItemId;
    private UUID chapterId;
    private UUID userId;
    private Boolean isReleased;
    private Boolean accessGranted;
}
