package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.assignment_group. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AssignmentGroupEntity extends BaseEntity {

    public static final String TABLE = "lms.assignment_group";

    private UUID tenantId;
    private UUID contentItemId;
    private UUID cohortId;
    private String name;
    private BigDecimal points;
    private Boolean isActive;
    private UUID discussionTopicId;
    private Boolean isReassigned;
}
