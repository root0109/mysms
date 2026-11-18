package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.ebook. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class EbookEntity extends BaseEntity {

    public static final String TABLE = "lms.ebook";

    private UUID tenantId;
    private UUID institutionId;
    private String title;
    private String description;
    private String ebookType;
    private UUID fileId;
    private UUID sourceFileId;
    private String conversionJobRef;
    private Boolean showRelatedMaterials;
    private String relatedMaterialSeed;
    private UUID categoryId;
    private Boolean isInRepository;
}
