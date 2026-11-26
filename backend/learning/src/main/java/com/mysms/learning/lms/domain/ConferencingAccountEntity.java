package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.conferencing_account. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ConferencingAccountEntity extends BaseEntity {

    public static final String TABLE = "lms.conferencing_account";

    private UUID tenantId;
    private String provider;
    private String accountName;
    private UUID ownerUserId;
    private Boolean isEnabled;
    private String externalAccountId;
    private String clientId;
    private String clientSecretEnc;
    private String externalCalendarId;
}
