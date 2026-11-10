package com.mysms.admission.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of admission.application_emergency_contact (column for column; the audit set comes from {@link BaseEntity}).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationEmergencyContactEntity extends BaseEntity {

    public static final String TABLE = "admission.application_emergency_contact";

    private UUID tenantId;
    private UUID applicationId;
    private Integer priority;
    private String contactNumber;
    private String relationship;
}
