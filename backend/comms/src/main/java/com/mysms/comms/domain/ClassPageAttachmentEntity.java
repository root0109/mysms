package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.class_page_attachment. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClassPageAttachmentEntity extends BaseEntity {

    public static final String TABLE = "comms.class_page_attachment";

    private UUID tenantId;
    private UUID classPageId;
    private UUID fileId;
    private Boolean isActive;
}
