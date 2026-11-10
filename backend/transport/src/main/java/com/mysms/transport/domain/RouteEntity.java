package com.mysms.transport.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of transport.route (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RouteEntity extends BaseEntity {

    public static final String TABLE = "transport.route";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String name;
    private String vehicleNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;
}
