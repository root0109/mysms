package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.mailbox_message. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MailboxMessageEntity extends BaseEntity {

    public static final String TABLE = "comms.mailbox_message";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID ownerUserId;
    private UUID aboutStudentId;
    private String folder;
    private String fromAddress;
    private String fromDisplay;
    private String toAddresses;
    private String subject;
    private String body;
    private String deliveryStatus;
    private String providerMessageId;
    private Boolean isProcessed;
    private OffsetDateTime processedAt;
    private OffsetDateTime dispatchedAt;
    private OffsetDateTime readAt;
}
