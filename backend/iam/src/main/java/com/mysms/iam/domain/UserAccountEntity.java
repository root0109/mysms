package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.user_account (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserAccountEntity extends BaseEntity {

    public static final String TABLE = "iam.user_account";

    private UUID tenantId;
    private String userType;
    private UUID personId;
    private String username;
    private String email;
    private String mobileNo;
    private String status;
    private OffsetDateTime emailVerifiedAt;
    private OffsetDateTime mobileVerifiedAt;
    private Boolean mustResetPassword;
    private UUID referredByUserId;
    private OffsetDateTime termsAcceptedAt;
    private String locale;
    private String timezone;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime accountExpiresAt;
    private Integer failedLoginCount;
    private UUID lastLoginRoleId;
    private OffsetDateTime registeredAt;
    private LocalDate joinedOn;
    private String externalRef;
    private String signature;
    private Boolean useSignature;
}
