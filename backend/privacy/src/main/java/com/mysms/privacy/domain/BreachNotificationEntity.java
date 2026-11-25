package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of privacy.breach_notification: append-only, so it is never updated and deletedAt stays null. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BreachNotificationEntity extends BaseEntity {

    public static final String TABLE = "privacy.breach_notification";

    private UUID tenantId;
    private UUID breachId;
    private String audience;
    private String kind;
    private OffsetDateTime sentAt;
    private UUID documentFileId;
}
