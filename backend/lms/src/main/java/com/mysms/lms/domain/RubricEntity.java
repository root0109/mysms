package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.rubric (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RubricEntity extends BaseEntity {

    public static final String TABLE = "lms.rubric";

    private UUID tenantId;
    private Integer version;
    private UUID supersededById;
    private OffsetDateTime publishedAt;
    private UUID institutionId;
    private String title;
    private Boolean isDefault;
}
