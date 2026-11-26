package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.release_rule_target. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReleaseRuleTargetEntity extends BaseEntity {

    public static final String TABLE = "lms.release_rule_target";

    private UUID tenantId;
    private UUID releaseRuleId;
    private UUID contentItemId;
    private UUID chapterId;
}
