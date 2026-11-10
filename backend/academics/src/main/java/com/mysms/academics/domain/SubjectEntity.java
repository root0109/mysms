package com.mysms.academics.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of academics.subject (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SubjectEntity extends BaseEntity {

    public static final String TABLE = "academics.subject";

    private UUID tenantId;
    private UUID institutionId;
    private UUID departmentId;
    private String code;
    private String name;
    private String deliveryType;
    private String defaultCategory;
    private BigDecimal gradeWeightage;
    private Boolean isActive;
}
