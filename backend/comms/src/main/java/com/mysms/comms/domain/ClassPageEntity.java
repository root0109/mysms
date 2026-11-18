package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.class_page. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClassPageEntity extends BaseEntity {

    public static final String TABLE = "comms.class_page";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private UUID pageCategoryId;
    private String name;
    private String description;
    private UUID coverImageFileId;
    private UUID ownerUserId;
    private Boolean isActive;
}
