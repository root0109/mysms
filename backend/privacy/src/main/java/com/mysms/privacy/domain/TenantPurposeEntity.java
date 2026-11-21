package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of privacy.tenant_purpose: the write side of a tenant's setting; the table has no soft delete. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TenantPurposeEntity extends BaseEntity {

    public static final String TABLE = "privacy.tenant_purpose";

    private UUID tenantId;
    private UUID purposeId;
    private Boolean enabled;
    private String maxScope;
    private UUID noticeId;
}
