package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.catalog_banner. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CatalogBannerEntity extends BaseEntity {

    public static final String TABLE = "lms.catalog_banner";

    private UUID tenantId;
    private UUID courseId;
    private String placement;
    private UUID imageFileId;
    private Integer sortOrder;
}
