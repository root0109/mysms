package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.course_subject (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CourseSubjectEntity extends BaseEntity {

    public static final String TABLE = "academics.course_subject";

    private UUID tenantId;
    private Integer termNumber;
    private BigDecimal credits;
    private String subjectCategory;
    private BigDecimal maxMarks;
    private UUID courseId;
    private UUID subjectId;
}
