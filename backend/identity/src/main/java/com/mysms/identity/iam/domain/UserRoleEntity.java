package com.mysms.identity.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.user_role. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserRoleEntity extends BaseEntity {

    public static final String TABLE = "iam.user_role";

    private UUID tenantId;
    private UUID userId;
    private UUID roleId;
    private String scopeType;
    private UUID scopeId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isActive;
}
