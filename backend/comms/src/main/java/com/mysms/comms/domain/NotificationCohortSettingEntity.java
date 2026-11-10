package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notification_cohort_setting (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationCohortSettingEntity extends BaseEntity {

    public static final String TABLE = "comms.notification_cohort_setting";

    private UUID tenantId;
    private UUID cohortId;
    private UUID notificationTypeId;
    private Boolean popupEnabled;
}
