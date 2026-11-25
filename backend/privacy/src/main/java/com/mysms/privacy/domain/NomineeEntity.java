package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.nominee: the person an adult principal names to exercise their rights on their death or incapacity.
 * The name and contact are held only as their encrypted envelopes; the table has no soft delete (a revoked nomination
 * stays as evidence), so deletedAt stays null.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NomineeEntity extends BaseEntity {

    public static final String TABLE = "privacy.nominee";

    private UUID tenantId;
    private UUID principalPersonId;
    private UUID nomineePersonId;
    private String nomineeNameEnc;
    private String nomineeContactEnc;
    private String status;
    private OffsetDateTime activatedAt;
    private UUID activatedByRequestId;
    private OffsetDateTime revokedAt;
}
