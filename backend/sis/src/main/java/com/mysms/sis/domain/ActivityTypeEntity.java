package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.activity_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ActivityTypeEntity extends BaseEntity {

    public static final String TABLE = "sis.activity_type";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private String name;
    private Boolean isActive;
}
