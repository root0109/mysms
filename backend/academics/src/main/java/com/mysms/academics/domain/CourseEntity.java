package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.course. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CourseEntity extends BaseEntity {

    public static final String TABLE = "academics.course";

    private UUID tenantId;
    private UUID institutionId;
    private UUID programId;
    private UUID departmentId;
    private UUID parentCourseId;
    private String code;
    private String name;
    private String evaluationType;
    private BigDecimal minUnitLoad;
    private BigDecimal maxUnitLoad;
    private Boolean isActive;
}
