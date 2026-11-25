package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.legal_hold; the table has no soft delete (a hold is evidence, released never deleted), so
 * deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LegalHoldEntity extends BaseEntity {

    public static final String TABLE = "privacy.legal_hold";

    private UUID tenantId;
    private String reasonCode;
    private String scope;
    private UUID subjectPersonId;
    private String categoryCode;
    private String module;
    private String entityType;
    private List<UUID> entityIds;
    private UUID placedByUserId;
    private OffsetDateTime placedAt;
    private LocalDate reviewBy;
    private OffsetDateTime releasedAt;
    private UUID releasedByUserId;
}
