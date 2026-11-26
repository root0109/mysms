package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.cohort. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CohortEntity extends BaseEntity {

    public static final String TABLE = "lms.cohort";

    private UUID tenantId;
    private UUID classSubjectId;
    private UUID batchId;
    private UUID academicYearId;
    private UUID institutionId;
    private String title;
    private String description;
    private String details;
    private UUID categoryId;
    private String medium;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;
    private Integer seatCount;
    private Boolean isPublished;
    private Boolean allowSelfEnrollment;
    private Boolean allowFutureAccess;
    private Boolean isPaid;
    private BigDecimal price;
    private UUID currencyId;
    private BigDecimal priceUsd;
    private Boolean showLiveClass;
    private Boolean evaluationEnabled;
    private LocalDate evaluationEndDate;
    private Boolean certificationEnabled;
    private String conferencingProvider;
    private UUID conferencingAccountId;
    private UUID timetableTemplateId;
    private UUID confirmationTemplateId;
    private UUID metaImageFileId;
    private UUID introVideoFileId;
    private String customComponent;
    private String customScript;
    private Boolean publishNotificationSent;
    private String code;
    private UUID academicTermId;
    private UUID programId;
    private String externalRef;
    private String lifecycleStatus;
    private OffsetDateTime staffAccessFrom;
    private OffsetDateTime staffAccessUntil;
    private OffsetDateTime studentAccessFrom;
    private OffsetDateTime studentAccessUntil;
    private Boolean hasSchedule;
    private Boolean staffCanEditSchedule;
    private String gradeDisplay;
    private UUID gradingSchemeId;
    private OffsetDateTime enrollmentOpensAt;
    private OffsetDateTime enrollmentClosesAt;
}
