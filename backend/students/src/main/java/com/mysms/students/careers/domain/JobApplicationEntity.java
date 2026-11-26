package com.mysms.students.careers.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of careers.job_application. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class JobApplicationEntity extends BaseEntity {

    public static final String TABLE = "careers.job_application";

    private UUID tenantId;
    private UUID jobOpeningId;
    private UUID applicantUserId;
    private UUID resumeFileId;
    private String status;
}
