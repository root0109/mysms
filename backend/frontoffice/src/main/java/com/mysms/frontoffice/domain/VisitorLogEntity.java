package com.mysms.frontoffice.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of frontoffice.visitor_log (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class VisitorLogEntity extends BaseEntity {

    public static final String TABLE = "frontoffice.visitor_log";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String visitorName;
    private String visitorType;
    private String email;
    private String phone;
    private String companyName;
    private String address;
    private UUID studentId;
    private String relationToStudent;
    private String relatedPersonName;
    private Integer visitorCount;
    private String purpose;
    private UUID hostStaffId;
    private LocalDate visitDate;
    private LocalTime entryTime;
    private LocalTime exitTime;
    private String remarks;
}
