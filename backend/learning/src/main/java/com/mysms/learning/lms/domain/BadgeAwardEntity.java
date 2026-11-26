package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.badge_award. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BadgeAwardEntity extends BaseEntity {

    public static final String TABLE = "lms.badge_award";

    private UUID tenantId;
    private UUID badgeId;
    private UUID userId;
    private LocalDate issuedOn;
}
