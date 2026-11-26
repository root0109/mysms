package com.mysms.identity.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of iam.user_account as the API shows it. The password hash is deliberately not part of this type: only the
 * sign-in repository reads it, into {@link com.mysms.identity.iam.repository.SignInAccount}, and it never leaves the
 * sign-in service.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MeAccountEntity extends BaseEntity {

    public static final String TABLE = "iam.user_account";

    private UUID tenantId;
    private UUID personId;
    private String username;
    private String email;
    private String mobileNo;
    private String status;
    private OffsetDateTime emailVerifiedAt;
    private OffsetDateTime mobileVerifiedAt;
    private boolean mustResetPassword;
    private UUID referredByUserId;
    private OffsetDateTime termsAcceptedAt;
    private String locale;
    private String timezone;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime accountExpiresAt;
    private int failedLoginCount;
    private UUID lastLoginRoleId;
    private OffsetDateTime registeredAt;
    private LocalDate joinedOn;
    private String externalRef;
    private String signature;
    private boolean useSignature;
    private String userType;
}
