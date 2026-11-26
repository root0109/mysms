package com.mysms.students.sis.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of sis.discipline_incident. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DisciplineIncidentEntity extends BaseEntity {

    public static final String TABLE = "sis.discipline_incident";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID classSectionId;
    private UUID studentId;
    private OffsetDateTime incidentAt;
    private UUID reportedByUserId;
    private String incidentType;
    private String details;
    private String response;
    private Boolean actionTaken;
    private String evidenceMediaType;
    private UUID evidenceFileId;
    private Boolean notifyGuardians;
    private Boolean seenByGuardian;
}
