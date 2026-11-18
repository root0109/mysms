package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.fee_structure. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FeeStructureEntity extends BaseEntity {

    public static final String TABLE = "finance.fee_structure";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID gradeLevelId;
    private UUID programId;
    private UUID feePlanId;
    private String name;
    private String status;
}
