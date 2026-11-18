package com.mysms.master.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of master.city. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CityEntity extends BaseEntity {

    public static final String TABLE = "master.city";

    private UUID countryId;
    private UUID stateProvinceId;
    private String name;
    private boolean active;
}
