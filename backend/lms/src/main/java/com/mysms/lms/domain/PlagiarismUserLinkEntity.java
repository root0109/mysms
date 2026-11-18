package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.plagiarism_user_link. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PlagiarismUserLinkEntity extends BaseEntity {

    public static final String TABLE = "lms.plagiarism_user_link";

    private UUID tenantId;
    private UUID userId;
    private UUID plagiarismAccountId;
    private String accountRef;
    private String providerUserRef;
}
