package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.peer_review. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PeerReviewEntity extends BaseEntity {

    public static final String TABLE = "lms.peer_review";

    private UUID tenantId;
    private UUID assignmentGroupId;
    private UUID reviewerUserId;
    private UUID revieweeUserId;
    private BigDecimal points;
}
