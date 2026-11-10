package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.mentorship (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MentorshipEntity extends BaseEntity {

    public static final String TABLE = "lms.mentorship";

    private UUID tenantId;
    private UUID mentorUserId;
    private UUID menteeUserId;
    private LocalDate validFrom;
    private LocalDate validTo;
}
