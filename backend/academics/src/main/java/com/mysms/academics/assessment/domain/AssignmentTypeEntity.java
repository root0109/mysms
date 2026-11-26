package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.assignment_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentTypeEntity extends BaseEntity {

    public static final String TABLE = "assessment.assignment_type";

    private UUID tenantId;
    private Integer sortOrder;
    private Boolean isActive;
    private String code;
    private String name;
    private String gradingBasis;
}
