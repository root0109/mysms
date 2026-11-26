package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.course. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LmsCourseEntity extends BaseEntity {

    public static final String TABLE = "lms.course";

    private UUID tenantId;
    private UUID institutionId;
    private String title;
    private String shortIntroduction;
    private String description;
    private UUID imageFileId;
    private String videoUrl;
    private UUID categoryId;
    private String status;
    private Boolean isPublished;
    private LocalDate publishedOn;
    private Boolean isUpcoming;
    private Boolean isFeatured;
    private Boolean disableSelfLearning;
    private Boolean enforceLessonOrder;
    private Boolean certificationEnabled;
    private Boolean paidCertificate;
    private UUID defaultEvaluatorId;
    private Boolean isPaid;
    private BigDecimal price;
    private UUID currencyId;
    private BigDecimal priceUsd;
    private String cardGradient;
    private String timezone;
    private Boolean publishNotificationSent;
    private Integer enrollmentCount;
    private Integer lessonCount;
    private BigDecimal averageRating;
    private String code;
    private String stateStandardCode;
    private BigDecimal credits;
    private String prerequisites;
    private String corequisites;
    private LocalDate effectiveFrom;
    private String lifecycleStatus;
    private LocalDate deactivatedOn;
    private Integer versionMajor;
    private Integer versionMinor;
    private UUID previousVersionId;
    private UUID rootCourseId;
    private String authorComments;
    private String externalRef;
    private Integer nominalDuration;
    private String deliveryModality;
    private Boolean isShared;
    private OffsetDateTime contentUpdatedAt;
    private String gradeDisplay;
    private UUID gradingSchemeId;
    private Boolean isInCatalog;
    private String catalogDescription;
    private OffsetDateTime catalogVisibleFrom;
    private OffsetDateTime catalogVisibleUntil;
    private String outline;
    private Integer workloadHours;
    private String catalogStatus;
}
