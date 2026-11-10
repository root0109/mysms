package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.user_device (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserDeviceEntity extends BaseEntity {

    public static final String TABLE = "iam.user_device";

    private UUID tenantId;
    private UUID userId;
    private String deviceIdentifier;
    private String platform;
    private String pushToken;
    private OffsetDateTime lastSeenAt;
    private Boolean isActive;
}
