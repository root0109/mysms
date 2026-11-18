package com.mysms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.disbursement_account. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DisbursementAccountEntity extends BaseEntity {

    public static final String TABLE = "payroll.disbursement_account";

    private UUID tenantId;
    private UUID institutionId;
    private UUID managedByUserId;
    private String name;
    private String accountKey;
    private String accountNumberEnc;
    private String bankRoutingCode;
    private String branchName;
    private String extraReference;
}
