package com.mysms.careers.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of careers.candidate_preference. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CandidatePreferenceEntity extends BaseEntity {

    public static final String TABLE = "careers.candidate_preference";

    private UUID tenantId;
    private UUID personId;
    private String openTo;
    private String preferredLocation;
    private String dreamCompanies;
    private String attire;
    private String collaboration;
    private String roleClarity;
    private String locationPreference;
    private String workHours;
    private String companyType;
}
