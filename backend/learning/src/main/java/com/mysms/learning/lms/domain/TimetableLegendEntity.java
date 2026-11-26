package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.timetable_legend. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TimetableLegendEntity extends BaseEntity {

    public static final String TABLE = "lms.timetable_legend";

    private UUID tenantId;
    private UUID cohortId;
    private UUID timetableTemplateId;
    private String itemKind;
    private String color;
    private String label;
}
