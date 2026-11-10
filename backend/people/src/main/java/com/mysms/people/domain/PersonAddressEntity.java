package com.mysms.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.person_address (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PersonAddressEntity extends BaseEntity {

    public static final String TABLE = "people.person_address";

    private UUID tenantId;
    private UUID personId;
    private String addressType;
    private String line1;
    private String line2;
    private UUID cityId;
    private String cityName;
    private UUID stateProvinceId;
    private UUID countryId;
    private String postalCode;
    private Boolean isPrimary;
    private String stateName;
    private Integer sortOrder;
}
