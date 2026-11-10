package com.mysms.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.person_document (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PersonDocumentEntity extends BaseEntity {

    public static final String TABLE = "people.person_document";

    private UUID tenantId;
    private UUID personId;
    private UUID institutionId;
    private String documentType;
    private String name;
    private UUID fileId;
    private Integer documentVersion;
    private String status;
}
