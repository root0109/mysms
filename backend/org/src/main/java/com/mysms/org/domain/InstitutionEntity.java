package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.institution (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class InstitutionEntity extends BaseEntity {

    public static final String TABLE = "org.institution";

    private UUID tenantId;
    private UUID parentInstitutionId;
    private String code;
    private String name;
    private String slug;
    private String institutionType;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private UUID cityId;
    private UUID stateProvinceId;
    private UUID countryId;
    private String postalCode;
    private String timezone;
    private UUID currencyId;
    private UUID logoFileId;
    private UUID signatureFileId;
    private String accreditation;
    private String approvalAuthority;
    private String status;
    private UUID institutionTypeId;
    private UUID themeId;
}
