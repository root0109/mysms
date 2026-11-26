package com.mysms.identity.iam.repository;

import java.util.UUID;

/** One permission code a role carries ({@link RoleAuthzRepository}). */
public record RolePermissionCodeRow(UUID roleId, String code) {
}
