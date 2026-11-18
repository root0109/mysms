package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.department. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DepartmentEntity extends BaseEntity {

    public static final String TABLE = "org.department";

    private UUID tenantId;
    private UUID institutionId;
    private UUID parentDepartmentId;
    private String code;
    private String name;
    private Boolean isActive;
}
