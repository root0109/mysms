package com.mysms.facility.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of facility.room_facility (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RoomFacilityEntity extends BaseEntity {

    public static final String TABLE = "facility.room_facility";

    private UUID tenantId;
    private UUID roomId;
    private UUID facilityId;
    private BigDecimal quantity;
}
