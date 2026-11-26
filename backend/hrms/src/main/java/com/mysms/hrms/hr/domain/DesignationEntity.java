package com.mysms.hrms.hr.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of hr.designation. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DesignationEntity extends BaseEntity {

    public static final String TABLE = "hr.designation";

    private UUID tenantId;
    private Integer sortOrder;
    private Boolean isActive;
    private String code;
    private String name;
    private String staffCategory;
}
