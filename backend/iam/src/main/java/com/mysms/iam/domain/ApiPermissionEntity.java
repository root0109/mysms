package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of the platform-level API-permission catalogue iam.api_permission: one operation of a deployed module and the
 * permission guarding it, as the module last published it (written by events only, read by platform staff).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApiPermissionEntity extends BaseEntity {

    public static final String TABLE = "iam.api_permission";

    private String moduleCode;
    private String operationId;
    private String httpMethod;
    private String pathTemplate;
    private String permissionCode;
    /** Space-separated security schemes (bearerAuth, platformAdminAuth). */
    private String schemes;
    private Boolean isAnonymous;
    private String audience;
    private String release;
    private String catalogueSha256;
    private OffsetDateTime firstSeenAt;
    private OffsetDateTime lastSeenAt;
    private OffsetDateTime retiredAt;
}
