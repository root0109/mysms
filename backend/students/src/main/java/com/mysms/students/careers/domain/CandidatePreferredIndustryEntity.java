package com.mysms.students.careers.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of careers.candidate_preferred_industry. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CandidatePreferredIndustryEntity extends BaseEntity {

    public static final String TABLE = "careers.candidate_preferred_industry";

    private UUID tenantId;
    private UUID candidatePreferenceId;
    private UUID industryId;
}
