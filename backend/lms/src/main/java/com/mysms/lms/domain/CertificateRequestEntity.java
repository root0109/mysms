package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.certificate_request. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CertificateRequestEntity extends BaseEntity {

    public static final String TABLE = "lms.certificate_request";

    private UUID tenantId;
    private UUID courseId;
    private UUID userId;
    private UUID cohortId;
    private UUID evaluatorUserId;
    private LocalDate requestDate;
    private String weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;
    private String meetingLinkEnc;
    private String status;
}
