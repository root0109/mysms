package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.related_course. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RelatedCourseEntity extends BaseEntity {

    public static final String TABLE = "lms.related_course";

    private UUID tenantId;
    private UUID courseId;
    private UUID relatedCourseId;
    private Integer sortOrder;
    private UUID authorProfileId;
}
