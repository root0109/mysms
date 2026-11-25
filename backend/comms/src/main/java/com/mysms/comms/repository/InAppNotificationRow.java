package com.mysms.comms.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A row of comms.in_app_notification. The table carries no audit set or row version (rows are written by the
 * notification fan-out and only ever marked read or archived), so it is read as a projection, not an entity.
 */
public record InAppNotificationRow(UUID id, UUID recipientUserId, OffsetDateTime readAt, OffsetDateTime createdAt,
        UUID notificationTypeId, String title, String body, String smsText, String actionUrl, UUID cohortId,
        String emailStatus, String smsStatus, String popupStatus, String digestStatus, OffsetDateTime archivedAt,
        String entityType, UUID entityId, OffsetDateTime updatedAt) {
}
