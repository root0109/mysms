package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.student_sibling. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StudentSiblingEntity extends BaseEntity {

    public static final String TABLE = "sis.student_sibling";

    private UUID tenantId;
    private UUID studentId;
    private UUID siblingStudentId;
    private String siblingName;
    private String details;
}
