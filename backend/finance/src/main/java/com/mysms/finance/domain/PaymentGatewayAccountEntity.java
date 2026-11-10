package com.mysms.finance.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of finance.payment_gateway_account (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PaymentGatewayAccountEntity extends BaseEntity {

    public static final String TABLE = "finance.payment_gateway_account";

    private UUID tenantId;
    private String keySecretEnc;
    private String webhookSecretEnc;
    private UUID institutionId;
    private String gatewayName;
    private String displayName;
    private String merchantId;
    private String keyId;
    private UUID currencyId;
    private Boolean isDefault;
    private Boolean isActive;
}
