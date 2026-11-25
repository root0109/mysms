package com.mysms.privacy.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection of a retention run (privacy.retention_run) with its policy's code and data category. */
public record RetentionRunView(UUID id, String policyCode, String categoryCode, String module, String action,
                               OffsetDateTime cutoff, String status, Integer affected, Integer held,
                               OffsetDateTime startedAt, OffsetDateTime completedAt) {
}
