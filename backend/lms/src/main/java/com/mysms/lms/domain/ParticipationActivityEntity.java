package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.participation_activity. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ParticipationActivityEntity extends BaseEntity {

    public static final String TABLE = "lms.participation_activity";

    private UUID tenantId;
    private UUID institutionId;
    private String title;
    private String description;
    private Boolean isInRepository;
}
