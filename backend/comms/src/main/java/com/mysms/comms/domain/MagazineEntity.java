package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.magazine. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MagazineEntity extends BaseEntity {

    public static final String TABLE = "comms.magazine";

    private UUID tenantId;
    private UUID institutionId;
    private UUID academicYearId;
    private String title;
    private Integer publicationYear;
    private UUID coverImageFileId;
    private UUID documentFileId;
}
