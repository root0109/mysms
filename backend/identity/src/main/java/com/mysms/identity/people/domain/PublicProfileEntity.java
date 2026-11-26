package com.mysms.identity.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.public_profile. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PublicProfileEntity extends BaseEntity {

    public static final String TABLE = "people.public_profile";

    private UUID tenantId;
    private UUID personId;
    private String headline;
    private String profession;
    private String personaCategory;
    private UUID coverImageFileId;
    private String city;
    private UUID countryId;
    private String college;
    private String branch;
    private String linkedinUrl;
    private String githubUrl;
    private String twitterUrl;
    private String mediumUrl;
    private Boolean hidePrivateInfo;
}
