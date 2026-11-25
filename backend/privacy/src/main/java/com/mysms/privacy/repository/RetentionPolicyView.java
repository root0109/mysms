package com.mysms.privacy.repository;

import java.util.UUID;

/**
 * Projection of a catalogue retention policy (privacy.retention_policy) with the tenant's setting of it
 * (privacy.tenant_retention_policy): the period and action in force — the tenant's when it set them, the catalogue's
 * otherwise — and the setting's version, 0 while never set.
 */
public record RetentionPolicyView(UUID id, String code, String categoryCode, String module, String trigger,
                                  int periodDays, int minimumPeriodDays, String action, Integer notifyBeforeDays,
                                  String legalReference, boolean overridden, int rowVersion) {
}
