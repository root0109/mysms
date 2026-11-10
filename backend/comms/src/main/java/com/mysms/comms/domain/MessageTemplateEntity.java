package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.message_template (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MessageTemplateEntity extends BaseEntity {

    public static final String TABLE = "comms.message_template";

    private UUID tenantId;
    private UUID institutionId;
    private String channel;
    private String code;
    private String name;
    private String subject;
    private String body;
    private String externalTemplateId;
    private Boolean isActive;
}
