package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.subject_registration_line. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SubjectRegistrationLineEntity extends BaseEntity {

    public static final String TABLE = "sis.subject_registration_line";

    private UUID tenantId;
    private UUID courseSubjectId;
    private UUID subjectRegistrationId;
    private UUID subjectId;
    private String selectionType;
}
