package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.exam_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExamTypeEntity extends BaseEntity {

    public static final String TABLE = "assessment.exam_type";

    private UUID tenantId;
    private Integer sortOrder;
    private Boolean isActive;
    private String code;
    private String name;
}
