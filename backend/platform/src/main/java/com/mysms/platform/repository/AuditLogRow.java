package com.mysms.platform.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One record of platform.audit_log as read back (a read-only projection: the trail is immutable and carries no audit
 * set of its own). {@code properties} is the stored JSON text: changed field names, reason, trace — never values.
 */
public record AuditLogRow(UUID id, String logName, String description, String event, String subjectType,
                          UUID subjectId, String causerType, UUID causerId, String properties, UUID batchId,
                          String ipAddress, OffsetDateTime occurredAt) {
}
