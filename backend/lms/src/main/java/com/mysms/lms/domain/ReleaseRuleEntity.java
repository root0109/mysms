package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.release_rule. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReleaseRuleEntity extends BaseEntity {

    public static final String TABLE = "lms.release_rule";

    private UUID tenantId;
    private UUID courseId;
    private UUID cohortId;
    private UUID sourceContentItemId;
    private String title;
    private Boolean stopIfTrue;
    private Boolean isDisabled;
    private Boolean reassignSource;
    private Integer sortOrder;
    private UUID copiedFromId;
}
