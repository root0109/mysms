package com.mysms.learning.lms.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One record of lms.access_log as read back (a read-only projection: the log is written once and carries no audit set
 * or version of its own).
 */
public record AccessLogRow(UUID id, UUID userId, OffsetDateTime accessedAt, String operation, String url,
                           String params, boolean isAsync, String sessionRef, UUID cohortId, String roleName,
                           String courseWeek, String analyticsLinkRef, String userAgent, String referer,
                           String ipAddress) {
}
