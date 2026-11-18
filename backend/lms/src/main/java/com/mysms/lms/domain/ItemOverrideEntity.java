package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.item_override. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ItemOverrideEntity extends BaseEntity {

    public static final String TABLE = "lms.item_override";

    private UUID tenantId;
    private UUID contentItemId;
    private UUID userId;
    private UUID groupId;
    private OffsetDateTime availableFrom;
    private OffsetDateTime dueAt;
    private Integer extraAttempts;
    private Integer extraTimeMinutes;
    private String reason;
}
