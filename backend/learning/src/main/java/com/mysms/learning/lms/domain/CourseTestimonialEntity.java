package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.course_testimonial. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CourseTestimonialEntity extends BaseEntity {

    public static final String TABLE = "lms.course_testimonial";

    private UUID tenantId;
    private UUID courseId;
    private String title;
    private String quote;
    private UUID photoFileId;
    private String currentPosition;
    private Integer sortOrder;
}
