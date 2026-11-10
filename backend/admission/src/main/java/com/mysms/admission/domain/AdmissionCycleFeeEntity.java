package com.mysms.admission.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of admission.admission_cycle_fee (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AdmissionCycleFeeEntity extends BaseEntity {

    public static final String TABLE = "admission.admission_cycle_fee";

    private UUID tenantId;
    private UUID admissionCycleId;
    private UUID courseId;
    private UUID feeHeadId;
}
