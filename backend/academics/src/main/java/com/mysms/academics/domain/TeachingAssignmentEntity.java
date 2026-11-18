package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.teaching_assignment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TeachingAssignmentEntity extends BaseEntity {

    public static final String TABLE = "academics.teaching_assignment";

    private UUID tenantId;
    private UUID classSubjectId;
    private UUID staffId;
    private Integer periodsPerWeek;
    private Integer periodsRemaining;
    private String teacherRole;
    private Boolean isActive;
}
