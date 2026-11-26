package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.media. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MediaEntity extends BaseEntity {

    public static final String TABLE = "library.media";

    private UUID tenantId;
    private UUID institutionId;
    private UUID acquiredAcademicYearId;
    private String title;
    private String isbn;
    private String internalCode;
    private String edition;
    private String description;
    private UUID mediaTypeId;
    private UUID mediaCategoryId;
    private UUID coverImageFileId;
    private Boolean isActive;
}
