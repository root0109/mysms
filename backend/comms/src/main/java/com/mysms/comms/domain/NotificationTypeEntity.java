package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notification_type. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NotificationTypeEntity extends BaseEntity {

    public static final String TABLE = "comms.notification_type";
    private String code;
    private String description;
    private String moduleCode;
    private String roleCode;
    private Boolean isEnabled;
    private Boolean popupEnabled;
    private Boolean teacherPopupEnabled;
    private String validParameters;
}
