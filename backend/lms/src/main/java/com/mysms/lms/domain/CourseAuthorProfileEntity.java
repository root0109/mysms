package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.course_author_profile (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CourseAuthorProfileEntity extends BaseEntity {

    public static final String TABLE = "lms.course_author_profile";

    private UUID tenantId;
    private UUID courseId;
    private String name;
    private String bio;
    private UUID photoFileId;
    private String qualification;
    private Integer sortOrder;
}
