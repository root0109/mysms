package com.mysms.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.result_template_band (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ResultTemplateBandEntity extends BaseEntity {

    public static final String TABLE = "assessment.result_template_band";

    private UUID tenantId;
    private UUID resultTemplateId;
    private UUID gradingBandId;
}
