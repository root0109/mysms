package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.grading_scheme, including its encoded criteria and institution scope. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GradingSchemeEntity extends BaseEntity {

    public static final String TABLE = "assessment.grading_scheme";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String name;
    private String schemeType;
    private String gradingMethod;
    private String criteria;
    private Boolean isActive;
    private Boolean isTemplate;
}
