package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.class_section. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClassSectionEntity extends BaseEntity {

    public static final String TABLE = "academics.class_section";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID gradeLevelId;
    private UUID sectionId;
    private UUID classTeacherStaffId;
    private Integer capacity;
    private String stream;
    private Boolean isActive;
}
