package com.mysms.admission.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of admission.application_guardian. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationGuardianEntity extends BaseEntity {

    public static final String TABLE = "admission.application_guardian";

    private UUID tenantId;
    private UUID applicationId;
    private String relationship;
    private String name;
    private UUID qualificationId;
    private String designation;
    private String occupation;
    private String organisation;
    private BigDecimal annualIncome;
    private String mobile;
    private String email;
    private String nationalIdEnc;
    private UUID photoFileId;
}
