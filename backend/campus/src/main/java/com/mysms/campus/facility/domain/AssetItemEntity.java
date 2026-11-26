package com.mysms.campus.facility.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of facility.asset_item. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssetItemEntity extends BaseEntity {

    public static final String TABLE = "facility.asset_item";

    private UUID tenantId;
    private Integer sortOrder;
    private Boolean isActive;
    private UUID institutionId;
    private String code;
    private String name;
    private String unitOfMeasure;
}
