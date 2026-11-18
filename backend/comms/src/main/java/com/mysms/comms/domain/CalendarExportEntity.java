package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.calendar_export. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CalendarExportEntity extends BaseEntity {

    public static final String TABLE = "comms.calendar_export";

    private UUID tenantId;
    private UUID userId;
    private String provider;
    private UUID cohortId;
    private String entityType;
    private UUID entityId;
    private String propertyName;
    private String propertyValue;
    private Boolean isDeleted;
    private OffsetDateTime lastExportedAt;
}
