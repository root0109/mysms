package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.annotation_share. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AnnotationShareEntity extends BaseEntity {

    public static final String TABLE = "lms.annotation_share";

    private UUID tenantId;
    private UUID annotationId;
    private UUID sharedWithUserId;
    private OffsetDateTime sharedAt;
}
