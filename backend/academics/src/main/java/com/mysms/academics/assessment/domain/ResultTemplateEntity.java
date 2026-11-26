package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.result_template. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ResultTemplateEntity extends BaseEntity {

    public static final String TABLE = "assessment.result_template";

    private UUID tenantId;
    private UUID examSessionId;
    private UUID gradingSchemeId;
    private String name;
    private LocalDate resultDate;
    private String status;
}
