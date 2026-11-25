package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.message_receipt: one recipient's read and hidden state of a conversation message. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MessageReceiptEntity extends BaseEntity {

    public static final String TABLE = "comms.message_receipt";

    private UUID tenantId;
    private UUID messageId;
    private UUID userId;
    private OffsetDateTime seenAt;
    private OffsetDateTime deletedAtByRecipient;
}
