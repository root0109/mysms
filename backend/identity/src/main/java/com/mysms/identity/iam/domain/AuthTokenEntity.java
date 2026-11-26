package com.mysms.identity.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A personal API token of iam.auth_token (token_type 'api'), column for column except the hash, which never leaves
 * the repository; the audit set comes from {@link BaseEntity}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AuthTokenEntity extends BaseEntity {

    public static final String TABLE = "iam.auth_token";

    private UUID tenantId;
    private UUID userId;
    private String tokenType;
    private String name;
    private String sentTo;
    private List<String> scopes;
    private String ipAddress;
    private OffsetDateTime expiresAt;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime revokedAt;
}
