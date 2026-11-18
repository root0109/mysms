package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notification_rule. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationRuleEntity extends BaseEntity {

    public static final String TABLE = "comms.notification_rule";

    private UUID tenantId;
    private UUID roleId;
    private UUID notificationTypeId;
    private UUID permissionId;
    private Boolean isEnabled;
    private String emailSubjectTemplate;
    private String emailBodyTemplate;
    private String smsTemplate;
}
