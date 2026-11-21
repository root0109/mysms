package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of privacy.consent_record; the table has no soft delete (decisions are kept), so deletedAt stays null. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ConsentRecordEntity extends BaseEntity {

    public static final String TABLE = "privacy.consent_record";

    private UUID tenantId;
    private UUID subjectPersonId;
    private UUID giverPersonId;
    private String giverCapacity;
    private String purposeCode;
    private String scope;
    private String decision;
    private UUID noticeVersionId;
    private String noticeLanguage;
    private OffsetDateTime validFrom;
    private LocalDate validUntil;
    private OffsetDateTime withdrawnAt;
    private UUID supersededById;
    private String channel;
    private String verificationStatus;
    private String verificationMethod;
    private UUID verifiedByUserId;
    private OffsetDateTime verifiedAt;
    private UUID recordedByUserId;
    private UUID evidenceFileId;
    private String externalArtefactId;
}
