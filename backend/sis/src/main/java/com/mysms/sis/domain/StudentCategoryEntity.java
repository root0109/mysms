package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.student_category. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentCategoryEntity extends BaseEntity {

    public static final String TABLE = "sis.student_category";

    private UUID tenantId;
    private Integer sortOrder;
    private Boolean isActive;
    private UUID institutionId;
    private String code;
    private String name;
}
