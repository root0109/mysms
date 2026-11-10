package com.mysms.careers.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of careers.job_opening (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class JobOpeningEntity extends BaseEntity {

    public static final String TABLE = "careers.job_opening";

    private UUID tenantId;
    private UUID postedByUserId;
    private String title;
    private String location;
    private UUID countryId;
    private String employmentType;
    private String workMode;
    private String status;
    private String description;
    private String companyName;
    private String companyWebsite;
    private String companyEmail;
    private UUID companyLogoFileId;
}
