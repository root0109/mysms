package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.event. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EventEntity extends BaseEntity {

    public static final String TABLE = "comms.event";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String audience;
    private UUID classSectionId;
    private String audienceLabel;
    private String title;
    private String description;
    private String category;
    private String location;
    private String organisedBy;
    private UUID imageFileId;
    private String color;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Boolean isAllDay;
    private Integer repeatCount;
    private Integer repeatInterval;
    private String repeatUnit;
    private String url;
    private String status;
    private UUID cohortId;
    private UUID ownerUserId;
    private String recurrenceRule;
    private Boolean forAllMembers;
    private Boolean staffCanEdit;
    private String importance;
}
