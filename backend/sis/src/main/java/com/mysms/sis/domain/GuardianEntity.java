package com.mysms.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.guardian. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GuardianEntity extends BaseEntity {

    public static final String TABLE = "sis.guardian";

    private UUID tenantId;
    private UUID personId;
    private UUID userId;
    private String profession;
    private String subOccupation;
    private String designation;
    private String organizationName;
    private BigDecimal annualIncome;
    private UUID qualificationId;
    private String mobile;
    private String email;
}
