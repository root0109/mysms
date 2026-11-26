package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.release_rule_condition. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReleaseRuleConditionEntity extends BaseEntity {

    public static final String TABLE = "lms.release_rule_condition";

    private UUID tenantId;
    private String conditionType;
    private UUID releaseRuleId;
    private String operator;
    private String value;
    private String value2;
    private Integer sortOrder;
}
