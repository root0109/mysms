package com.mysms.platform.master.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of master.state_province. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StateProvinceEntity extends BaseEntity {

    public static final String TABLE = "master.state_province";

    private UUID countryId;
    private String code;
    private String name;
    private boolean active;
}
