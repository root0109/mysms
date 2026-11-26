package com.mysms.campus.transport.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of transport.route_stop. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RouteStopEntity extends BaseEntity {

    public static final String TABLE = "transport.route_stop";

    private UUID tenantId;
    private UUID routeId;
    private Integer sequence;
    private String stopName;
    private LocalTime pickupTime;
    private LocalTime dropTime;
}
