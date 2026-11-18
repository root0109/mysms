package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.entity_note. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EntityNoteEntity extends BaseEntity {

    public static final String TABLE = "platform.entity_note";

    private UUID tenantId;
    private UUID institutionId;
    private String entityType;
    private UUID entityId;
    private String noteText;
}
