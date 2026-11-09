package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.setting_value: one setting's value at one scope of a tenant. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SettingValueEntity extends BaseEntity {

    public static final String TABLE = "iam.setting_value";

    private UUID tenantId;
    private String scopeType;
    private UUID scopeId;
    private String settingKey;
    private String value;
    private boolean locked;
}
