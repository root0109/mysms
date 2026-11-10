package com.mysms.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.certification (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CertificationEntity extends BaseEntity {

    public static final String TABLE = "people.certification";

    private UUID tenantId;
    private UUID personId;
    private String name;
    private String issuingOrganization;
    private LocalDate issueDate;
    private Boolean expires;
    private LocalDate expirationDate;
    private String description;
}
