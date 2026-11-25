package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.breach; the table has no soft delete (the register is evidence of how a breach was handled), so
 * deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BreachEntity extends BaseEntity {

    public static final String TABLE = "privacy.breach";

    private UUID tenantId;
    private String number;
    private String status;
    private OffsetDateTime detectedAt;
    private OffsetDateTime becameAwareAt;
    private OffsetDateTime boardReportDueAt;
    private String description;
    private List<String> categories;
    private List<String> modules;
    private Integer estimatedPrincipals;
    private String likelyConsequences;
    private String mitigation;
    private UUID ownerUserId;
    private OffsetDateTime boardReportSentAt;
    private String notABreachReason;
    private OffsetDateTime closedAt;
}
