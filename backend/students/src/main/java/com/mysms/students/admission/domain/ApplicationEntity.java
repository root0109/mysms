package com.mysms.students.admission.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of admission.application. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ApplicationEntity extends BaseEntity {

    public static final String TABLE = "admission.application";

    private UUID tenantId;
    private UUID institutionId;
    private UUID admissionCycleId;
    private String applicationNumber;
    private OffsetDateTime applicationDate;
    private LocalDate admissionDate;
    private UUID academicYearId;
    private UUID gradeLevelId;
    private UUID sectionId;
    private UUID programId;
    private UUID courseId;
    private UUID batchId;
    private UUID applicantPersonId;
    private UUID studentId;
    private Boolean isExistingStudent;
    private UUID honorificId;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String birthPlace;
    private String nationality;
    private String religion;
    private String socialCategory;
    private String motherTongue;
    private String identificationMarks;
    private String nationalIdEnc;
    private String bloodGroup;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private UUID photoFileId;
    private String email;
    private String phone;
    private String mobile;
    private String addressLine1;
    private String addressLine2;
    private String cityName;
    private String postalCode;
    private UUID stateProvinceId;
    private UUID countryId;
    private String permanentAddress;
    private String communicationAddress;
    private String previousInstitution;
    private String previousCourse;
    private String previousResult;
    private String previousBoard;
    private String boardRegistrationNumber;
    private String reasonForLeaving;
    private String languageChoice;
    private String streamChoice;
    private Boolean hasSiblings;
    private Boolean hasMedicalHistory;
    private String medicalDetails;
    private Boolean hasExtracurricular;
    private String extracurricularDetails;
    private String transportMode;
    private String transportDetails;
    private String familyBusiness;
    private BigDecimal familyIncome;
    private BigDecimal applicationFee;
    private BigDecimal discountPercent;
    private UUID feePlanId;
    private LocalDate feeStartDate;
    private LocalDate paymentDueDate;
    private String paymentStatus;
    private String status;
    private String remarks;
    private String paymentStatusNote;
}
