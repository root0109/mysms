package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import com.mysms.privacy.model.RightsRequestDetails;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of privacy.rights_request; the table has no soft delete (a request is evidence of how a right was honoured),
 * so deletedAt stays null. outcome_file_id is not mapped: only the fulfilment of a later phase writes and reads it;
 * neither is access_summary, which the repository writes and reads by its own statements.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RightsRequestEntity extends BaseEntity {

    public static final String TABLE = "privacy.rights_request";

    private UUID tenantId;
    private String number;
    private String type;
    private UUID subjectPersonId;
    private UUID requesterPersonId;
    private String requesterCapacity;
    private String status;
    private String identityCheck;
    private RightsRequestDetails details;
    private UUID assigneeUserId;
    private OffsetDateTime submittedAt;
    private OffsetDateTime dueAt;
    private String extendedReason;
    private OffsetDateTime closedAt;
    private String outcomeCode;
    private String outcomeNote;
    private String outcomeResponse;
}
