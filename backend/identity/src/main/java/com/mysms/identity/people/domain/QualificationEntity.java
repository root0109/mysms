package com.mysms.identity.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.qualification. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class QualificationEntity extends BaseEntity {

    public static final String TABLE = "people.qualification";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private String name;
    private String level;
    private Boolean isActive;
}
