package com.mysms.campus.frontoffice.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of frontoffice.call_log. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CallLogEntity extends BaseEntity {

    public static final String TABLE = "frontoffice.call_log";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String direction;
    private String purpose;
    private String callerName;
    private String incomingNumber;
    private String outgoingNumber;
    private LocalDate callDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationSeconds;
    private String description;
    private UUID loggedByUserId;
}
