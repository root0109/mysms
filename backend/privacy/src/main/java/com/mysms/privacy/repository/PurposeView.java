package com.mysms.privacy.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projection of a catalogue purpose (privacy.purpose) with the tenant's setting of it (privacy.tenant_purpose); without
 * a tenant row the purpose is switched off, has no ceiling or notice, and the setting's version is 0.
 */
public record PurposeView(UUID id, String code, String scopeKind, String fiduciary, boolean forbiddenForMinors,
                          String majorityGrace, Integer defaultValidMonths, String status, int version, boolean enabled,
                          String maxScope, UUID noticeId, OffsetDateTime updatedAt, int rowVersion) {
}
