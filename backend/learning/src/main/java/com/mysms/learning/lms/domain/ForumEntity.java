package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumEntity extends BaseEntity {

    public static final String TABLE = "lms.forum";

    private UUID tenantId;
    private UUID cohortId;
    private String title;
    private String description;
    private OffsetDateTime earlyAccessFrom;
    private OffsetDateTime opensAt;
    private OffsetDateTime closesAt;
    private OffsetDateTime lateAccessUntil;
}
