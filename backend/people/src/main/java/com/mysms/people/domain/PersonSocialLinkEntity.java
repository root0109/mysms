package com.mysms.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.person_social_link (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PersonSocialLinkEntity extends BaseEntity {

    public static final String TABLE = "people.person_social_link";

    private UUID tenantId;
    private UUID personId;
    private String network;
    private String handle;
    private Integer sortOrder;
}
