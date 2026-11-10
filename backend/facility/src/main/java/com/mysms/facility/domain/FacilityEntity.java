package com.mysms.facility.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of facility.facility (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FacilityEntity extends BaseEntity {

    public static final String TABLE = "facility.facility";

    private UUID tenantId;
    private Integer sortOrder;
    private UUID institutionId;
    private String code;
    private String name;
    private Boolean isActive;
}
