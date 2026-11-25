package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.tenant_retention_policy: the tenant's own period and action of a catalogue retention policy;
 * the table has no soft delete, so deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TenantRetentionPolicyEntity extends BaseEntity {

    public static final String TABLE = "privacy.tenant_retention_policy";

    private UUID tenantId;
    private UUID policyId;
    private Integer periodDays;
    private String action;
}
