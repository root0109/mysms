package com.mysms.campus.facility.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of facility.room_asset. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RoomAssetEntity extends BaseEntity {

    public static final String TABLE = "facility.room_asset";

    private UUID tenantId;
    private UUID roomId;
    private UUID assetItemId;
    private String tagCode;
    private BigDecimal quantity;
}
