package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.plagiarism_account. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PlagiarismAccountEntity extends BaseEntity {

    public static final String TABLE = "lms.plagiarism_account";

    private UUID tenantId;
    private String provider;
    private String accountRef;
    private String subAccountRef;
    private String apiUrl;
    private String sharedSecretEnc;
    private Boolean isDefault;
    private UUID defaultTeacherUserId;
    private String defaultTeacherEmail;
    private String defaultTeacherFirstName;
    private String defaultTeacherLastName;
}
