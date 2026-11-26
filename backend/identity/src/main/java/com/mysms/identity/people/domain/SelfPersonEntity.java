package com.mysms.identity.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of people.person as the API shows it. The encrypted national id (national_id_enc) is deliberately not part
 * of this type: it is write-only in the contract and is read only by the key-management path.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SelfPersonEntity extends BaseEntity {

    public static final String TABLE = "people.person";

    private UUID tenantId;
    private UUID honorificId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String maritalStatus;
    private String birthPlace;
    private String nativePlace;
    private UUID nationalityCountryId;
    private String motherTongue;
    private String religion;
    private String socialCategory;
    private String socialSubCategory;
    private String nationalIdType;
    private String visaInfo;
    private String primaryEmail;
    private String primaryPhone;
    private String alternatePhone;
    private UUID photoFileId;
    private String remarks;
    private String workPhone;
    private String websiteUrl;
    private String aboutMe;
}
