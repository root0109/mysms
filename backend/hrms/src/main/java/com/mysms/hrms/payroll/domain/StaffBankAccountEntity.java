package com.mysms.hrms.payroll.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of payroll.staff_bank_account. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class StaffBankAccountEntity extends BaseEntity {

    public static final String TABLE = "payroll.staff_bank_account";

    private UUID tenantId;
    private String accountNumberEnc;
    private UUID staffId;
    private String accountHolderName;
    private String accountNumberMasked;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private Boolean isPrimary;
    private Boolean isActive;
}
