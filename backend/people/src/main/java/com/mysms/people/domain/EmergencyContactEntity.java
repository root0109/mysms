package com.mysms.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.emergency_contact. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EmergencyContactEntity extends BaseEntity {

    public static final String TABLE = "people.emergency_contact";

    private UUID tenantId;
    private UUID personId;
    private UUID contactPersonId;
    private String contactName;
    private String relationship;
    private String phone;
    private Integer priority;
}
