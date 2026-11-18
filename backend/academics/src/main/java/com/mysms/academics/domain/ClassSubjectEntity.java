package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.class_subject. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClassSubjectEntity extends BaseEntity {

    public static final String TABLE = "academics.class_subject";

    private UUID tenantId;
    private UUID classSectionId;
    private UUID subjectId;
    private String offeringType;
    private Boolean isActive;
}
