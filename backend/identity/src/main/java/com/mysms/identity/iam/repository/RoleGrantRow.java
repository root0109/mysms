package com.mysms.identity.iam.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * One active role grant of a user (iam.user_role joined with iam.role).
 *
 * @param roleCode  the role's code
 * @param scopeType tenant, institution, department, class_section, course, lms_course or lms_cohort
 * @param scopeId   the scoped unit; empty for tenant-wide grants
 * @param primary   the user's primary unit of this scope type (settings resolve through the primary department and
 *                  class section)
 */
public record RoleGrantRow(String roleCode, String scopeType, Optional<UUID> scopeId, boolean primary) {

    public static final String TENANT = "tenant";

    public RoleGrantRow {
        scopeId = scopeId == null ? Optional.empty() : scopeId;
    }

    /** The `roles` claim form: {@code code@tenant} or {@code code@scopeType:scopeId}. */
    public String claim() {
        return roleCode + "@" + scopeType + scopeId.map(id -> ":" + id).orElse("");
    }
}
