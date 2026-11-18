package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.marksheet. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MarksheetEntity extends BaseEntity {

    public static final String TABLE = "assessment.marksheet";

    private UUID tenantId;
    private OffsetDateTime publishedAt;
    private UUID publishedByUserId;
    private Boolean visibleToGuardians;
    private UUID examSessionId;
    private UUID resultTemplateId;
    private String name;
    private LocalDate generatedOn;
    private UUID generatedByUserId;
    private String status;
    private Integer totalPassed;
    private Integer totalFailed;
}
