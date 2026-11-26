package com.mysms.students.careers.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of careers.industry. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class IndustryEntity extends BaseEntity {

    public static final String TABLE = "careers.industry";

    private UUID tenantId;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private String name;
}
