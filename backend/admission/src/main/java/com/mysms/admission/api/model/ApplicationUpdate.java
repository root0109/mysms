// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Application that can be updated (partial update; see updateMask).
 */

public class ApplicationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID admissionCycleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String applicationNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime applicationDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate admissionDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID sectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID applicantPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isExistingStudent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID honorificId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String fullName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String firstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String middleName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String lastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateOfBirth;

  /**
   * Gender.
   */
  public enum GenderEnum {
    MALE("male"),
    
    FEMALE("female"),
    
    OTHER("other");

    private final String value;

    GenderEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static GenderEnum fromValue(String value) {
      for (GenderEnum b : GenderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GenderEnum gender;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String birthPlace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationality;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String religion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String socialCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String motherTongue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String identificationMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationalId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bloodGroup;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String heightCm;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String weightKg;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String addressLine1;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String addressLine2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String cityName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String postalCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID stateProvinceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String permanentAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String communicationAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String previousInstitution;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String previousCourse;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String previousResult;

  /**
   * Previous examination board.
   */
  public enum PreviousBoardEnum {
    CBSE("cbse"),
    
    STATE_BOARD("state_board"),
    
    ICSE("icse"),
    
    MATRICULATION("matriculation"),
    
    ANGLO_INDIAN("anglo_indian"),
    
    OTHER("other");

    private final String value;

    PreviousBoardEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PreviousBoardEnum fromValue(String value) {
      for (PreviousBoardEnum b : PreviousBoardEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PreviousBoardEnum previousBoard;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String boardRegistrationNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reasonForLeaving;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String languageChoice;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String streamChoice;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasSiblings;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasMedicalHistory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String medicalDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasExtracurricular;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String extracurricularDetails;

  /**
   * Mode of transport.
   */
  public enum TransportModeEnum {
    AUTO("auto"),
    
    CAR("car"),
    
    CITY_BUS("city_bus"),
    
    CYCLE("cycle"),
    
    RICKSHAW("rickshaw"),
    
    SCHOOL_BUS("school_bus"),
    
    TAXI("taxi"),
    
    WALKING("walking");

    private final String value;

    TransportModeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TransportModeEnum fromValue(String value) {
      for (TransportModeEnum b : TransportModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TransportModeEnum transportMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String transportDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String familyBusiness;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String familyIncome;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String applicationFee;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feePlanId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate feeStartDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate paymentDueDate;

  /**
   * Fee payment state.
   */
  public enum PaymentStatusEnum {
    UNPAID("unpaid"),
    
    PARTIAL("partial"),
    
    PAID("paid"),
    
    WAIVED("waived");

    private final String value;

    PaymentStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PaymentStatusEnum fromValue(String value) {
      for (PaymentStatusEnum b : PaymentStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PaymentStatusEnum paymentStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String paymentStatusNote;

  public ApplicationUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public ApplicationUpdate admissionCycleId(@Nullable UUID admissionCycleId) {
    this.admissionCycleId = admissionCycleId;
    return this;
  }

  /**
   * Cycle applied in.
   * @return admissionCycleId
   */
  @Valid 
  @JsonProperty("admissionCycleId")
  public @Nullable UUID getAdmissionCycleId() {
    return admissionCycleId;
  }

  @JsonProperty("admissionCycleId")
  public void setAdmissionCycleId(@Nullable UUID admissionCycleId) {
    this.admissionCycleId = admissionCycleId;
  }

  public ApplicationUpdate applicationNumber(@Nullable String applicationNumber) {
    this.applicationNumber = applicationNumber;
    return this;
  }

  /**
   * Application number.
   * @return applicationNumber
   */
  @Size(max = 30) 
  @JsonProperty("applicationNumber")
  public @Nullable String getApplicationNumber() {
    return applicationNumber;
  }

  @JsonProperty("applicationNumber")
  public void setApplicationNumber(@Nullable String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public ApplicationUpdate applicationDate(@Nullable OffsetDateTime applicationDate) {
    this.applicationDate = applicationDate;
    return this;
  }

  /**
   * Submitted on.
   * @return applicationDate
   */
  @Valid 
  @JsonProperty("applicationDate")
  public @Nullable OffsetDateTime getApplicationDate() {
    return applicationDate;
  }

  @JsonProperty("applicationDate")
  public void setApplicationDate(@Nullable OffsetDateTime applicationDate) {
    this.applicationDate = applicationDate;
  }

  public ApplicationUpdate admissionDate(@Nullable LocalDate admissionDate) {
    this.admissionDate = admissionDate;
    return this;
  }

  /**
   * Date admitted.
   * @return admissionDate
   */
  @Valid 
  @JsonProperty("admissionDate")
  public @Nullable LocalDate getAdmissionDate() {
    return admissionDate;
  }

  @JsonProperty("admissionDate")
  public void setAdmissionDate(@Nullable LocalDate admissionDate) {
    this.admissionDate = admissionDate;
  }

  public ApplicationUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year applied for.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public ApplicationUpdate gradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade applied for.
   * @return gradeLevelId
   */
  @Valid 
  @JsonProperty("gradeLevelId")
  public @Nullable UUID getGradeLevelId() {
    return gradeLevelId;
  }

  @JsonProperty("gradeLevelId")
  public void setGradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
  }

  public ApplicationUpdate sectionId(@Nullable UUID sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  /**
   * Section allotted.
   * @return sectionId
   */
  @Valid 
  @JsonProperty("sectionId")
  public @Nullable UUID getSectionId() {
    return sectionId;
  }

  @JsonProperty("sectionId")
  public void setSectionId(@Nullable UUID sectionId) {
    this.sectionId = sectionId;
  }

  public ApplicationUpdate programId(@Nullable UUID programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program applied for.
   * @return programId
   */
  @Valid 
  @JsonProperty("programId")
  public @Nullable UUID getProgramId() {
    return programId;
  }

  @JsonProperty("programId")
  public void setProgramId(@Nullable UUID programId) {
    this.programId = programId;
  }

  public ApplicationUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course applied for.
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public ApplicationUpdate batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch applied for.
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public ApplicationUpdate applicantPersonId(@Nullable UUID applicantPersonId) {
    this.applicantPersonId = applicantPersonId;
    return this;
  }

  /**
   * Applicant's person record once created.
   * @return applicantPersonId
   */
  @Valid 
  @JsonProperty("applicantPersonId")
  public @Nullable UUID getApplicantPersonId() {
    return applicantPersonId;
  }

  @JsonProperty("applicantPersonId")
  public void setApplicantPersonId(@Nullable UUID applicantPersonId) {
    this.applicantPersonId = applicantPersonId;
  }

  public ApplicationUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student created from this application.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public ApplicationUpdate isExistingStudent(@Nullable Boolean isExistingStudent) {
    this.isExistingStudent = isExistingStudent;
    return this;
  }

  /**
   * Applicant is already a student of the tenant.
   * @return isExistingStudent
   */
  
  @JsonProperty("isExistingStudent")
  public @Nullable Boolean getIsExistingStudent() {
    return isExistingStudent;
  }

  @JsonProperty("isExistingStudent")
  public void setIsExistingStudent(@Nullable Boolean isExistingStudent) {
    this.isExistingStudent = isExistingStudent;
  }

  public ApplicationUpdate honorificId(@Nullable UUID honorificId) {
    this.honorificId = honorificId;
    return this;
  }

  /**
   * Name prefix.
   * @return honorificId
   */
  @Valid 
  @JsonProperty("honorificId")
  public @Nullable UUID getHonorificId() {
    return honorificId;
  }

  @JsonProperty("honorificId")
  public void setHonorificId(@Nullable UUID honorificId) {
    this.honorificId = honorificId;
  }

  public ApplicationUpdate fullName(@Nullable String fullName) {
    this.fullName = fullName;
    return this;
  }

  /**
   * Applicant full name.
   * @return fullName
   */
  @Size(max = 255) 
  @JsonProperty("fullName")
  public @Nullable String getFullName() {
    return fullName;
  }

  @JsonProperty("fullName")
  public void setFullName(@Nullable String fullName) {
    this.fullName = fullName;
  }

  public ApplicationUpdate firstName(@Nullable String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Given name (required for new applications by the application; NULL when only a full name was captured).
   * @return firstName
   */
  @Size(max = 100) 
  @JsonProperty("firstName")
  public @Nullable String getFirstName() {
    return firstName;
  }

  @JsonProperty("firstName")
  public void setFirstName(@Nullable String firstName) {
    this.firstName = firstName;
  }

  public ApplicationUpdate middleName(@Nullable String middleName) {
    this.middleName = middleName;
    return this;
  }

  /**
   * Middle name.
   * @return middleName
   */
  @Size(max = 100) 
  @JsonProperty("middleName")
  public @Nullable String getMiddleName() {
    return middleName;
  }

  @JsonProperty("middleName")
  public void setMiddleName(@Nullable String middleName) {
    this.middleName = middleName;
  }

  public ApplicationUpdate lastName(@Nullable String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Family name.
   * @return lastName
   */
  @Size(max = 100) 
  @JsonProperty("lastName")
  public @Nullable String getLastName() {
    return lastName;
  }

  @JsonProperty("lastName")
  public void setLastName(@Nullable String lastName) {
    this.lastName = lastName;
  }

  public ApplicationUpdate dateOfBirth(@Nullable LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  /**
   * Date of birth.
   * @return dateOfBirth
   */
  @Valid 
  @JsonProperty("dateOfBirth")
  public @Nullable LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public ApplicationUpdate gender(@Nullable GenderEnum gender) {
    this.gender = gender;
    return this;
  }

  /**
   * Gender.
   * @return gender
   */
  
  @JsonProperty("gender")
  public @Nullable GenderEnum getGender() {
    return gender;
  }

  @JsonProperty("gender")
  public void setGender(@Nullable GenderEnum gender) {
    this.gender = gender;
  }

  public ApplicationUpdate birthPlace(@Nullable String birthPlace) {
    this.birthPlace = birthPlace;
    return this;
  }

  /**
   * Place of birth.
   * @return birthPlace
   */
  @Size(max = 120) 
  @JsonProperty("birthPlace")
  public @Nullable String getBirthPlace() {
    return birthPlace;
  }

  @JsonProperty("birthPlace")
  public void setBirthPlace(@Nullable String birthPlace) {
    this.birthPlace = birthPlace;
  }

  public ApplicationUpdate nationality(@Nullable String nationality) {
    this.nationality = nationality;
    return this;
  }

  /**
   * Nationality.
   * @return nationality
   */
  @Size(max = 60) 
  @JsonProperty("nationality")
  public @Nullable String getNationality() {
    return nationality;
  }

  @JsonProperty("nationality")
  public void setNationality(@Nullable String nationality) {
    this.nationality = nationality;
  }

  public ApplicationUpdate religion(@Nullable String religion) {
    this.religion = religion;
    return this;
  }

  /**
   * Religion.
   * @return religion
   */
  @Size(max = 60) 
  @JsonProperty("religion")
  public @Nullable String getReligion() {
    return religion;
  }

  @JsonProperty("religion")
  public void setReligion(@Nullable String religion) {
    this.religion = religion;
  }

  public ApplicationUpdate socialCategory(@Nullable String socialCategory) {
    this.socialCategory = socialCategory;
    return this;
  }

  /**
   * Social category.
   * @return socialCategory
   */
  @Size(max = 30) 
  @JsonProperty("socialCategory")
  public @Nullable String getSocialCategory() {
    return socialCategory;
  }

  @JsonProperty("socialCategory")
  public void setSocialCategory(@Nullable String socialCategory) {
    this.socialCategory = socialCategory;
  }

  public ApplicationUpdate motherTongue(@Nullable String motherTongue) {
    this.motherTongue = motherTongue;
    return this;
  }

  /**
   * Mother tongue.
   * @return motherTongue
   */
  @Size(max = 60) 
  @JsonProperty("motherTongue")
  public @Nullable String getMotherTongue() {
    return motherTongue;
  }

  @JsonProperty("motherTongue")
  public void setMotherTongue(@Nullable String motherTongue) {
    this.motherTongue = motherTongue;
  }

  public ApplicationUpdate identificationMarks(@Nullable String identificationMarks) {
    this.identificationMarks = identificationMarks;
    return this;
  }

  /**
   * Identification marks.
   * @return identificationMarks
   */
  @Size(max = 255) 
  @JsonProperty("identificationMarks")
  public @Nullable String getIdentificationMarks() {
    return identificationMarks;
  }

  @JsonProperty("identificationMarks")
  public void setIdentificationMarks(@Nullable String identificationMarks) {
    this.identificationMarks = identificationMarks;
  }

  public ApplicationUpdate nationalId(@Nullable String nationalId) {
    this.nationalId = nationalId;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. National id, encrypted.
   * @return nationalId
   */
  
  @JsonProperty("nationalId")
  public @Nullable String getNationalId() {
    return nationalId;
  }

  @JsonProperty("nationalId")
  public void setNationalId(@Nullable String nationalId) {
    this.nationalId = nationalId;
  }

  public ApplicationUpdate bloodGroup(@Nullable String bloodGroup) {
    this.bloodGroup = bloodGroup;
    return this;
  }

  /**
   * Blood group.
   * @return bloodGroup
   */
  @Size(max = 5) 
  @JsonProperty("bloodGroup")
  public @Nullable String getBloodGroup() {
    return bloodGroup;
  }

  @JsonProperty("bloodGroup")
  public void setBloodGroup(@Nullable String bloodGroup) {
    this.bloodGroup = bloodGroup;
  }

  public ApplicationUpdate heightCm(@Nullable String heightCm) {
    this.heightCm = heightCm;
    return this;
  }

  /**
   * Height (cm).
   * @return heightCm
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("heightCm")
  public @Nullable String getHeightCm() {
    return heightCm;
  }

  @JsonProperty("heightCm")
  public void setHeightCm(@Nullable String heightCm) {
    this.heightCm = heightCm;
  }

  public ApplicationUpdate weightKg(@Nullable String weightKg) {
    this.weightKg = weightKg;
    return this;
  }

  /**
   * Weight (kg).
   * @return weightKg
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("weightKg")
  public @Nullable String getWeightKg() {
    return weightKg;
  }

  @JsonProperty("weightKg")
  public void setWeightKg(@Nullable String weightKg) {
    this.weightKg = weightKg;
  }

  public ApplicationUpdate photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Applicant photo.
   * @return photoFileId
   */
  @Valid 
  @JsonProperty("photoFileId")
  public @Nullable UUID getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
  }

  public ApplicationUpdate email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Applicant / family e-mail.
   * @return email
   */
  @Size(max = 320) 
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public ApplicationUpdate phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone.
   * @return phone
   */
  @Size(max = 20) 
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  public ApplicationUpdate mobile(@Nullable String mobile) {
    this.mobile = mobile;
    return this;
  }

  /**
   * Mobile.
   * @return mobile
   */
  @Size(max = 20) 
  @JsonProperty("mobile")
  public @Nullable String getMobile() {
    return mobile;
  }

  @JsonProperty("mobile")
  public void setMobile(@Nullable String mobile) {
    this.mobile = mobile;
  }

  public ApplicationUpdate addressLine1(@Nullable String addressLine1) {
    this.addressLine1 = addressLine1;
    return this;
  }

  /**
   * Address line 1.
   * @return addressLine1
   */
  @Size(max = 255) 
  @JsonProperty("addressLine1")
  public @Nullable String getAddressLine1() {
    return addressLine1;
  }

  @JsonProperty("addressLine1")
  public void setAddressLine1(@Nullable String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public ApplicationUpdate addressLine2(@Nullable String addressLine2) {
    this.addressLine2 = addressLine2;
    return this;
  }

  /**
   * Address line 2.
   * @return addressLine2
   */
  @Size(max = 255) 
  @JsonProperty("addressLine2")
  public @Nullable String getAddressLine2() {
    return addressLine2;
  }

  @JsonProperty("addressLine2")
  public void setAddressLine2(@Nullable String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public ApplicationUpdate cityName(@Nullable String cityName) {
    this.cityName = cityName;
    return this;
  }

  /**
   * City.
   * @return cityName
   */
  @Size(max = 120) 
  @JsonProperty("cityName")
  public @Nullable String getCityName() {
    return cityName;
  }

  @JsonProperty("cityName")
  public void setCityName(@Nullable String cityName) {
    this.cityName = cityName;
  }

  public ApplicationUpdate postalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * Postal code.
   * @return postalCode
   */
  @Size(max = 12) 
  @JsonProperty("postalCode")
  public @Nullable String getPostalCode() {
    return postalCode;
  }

  @JsonProperty("postalCode")
  public void setPostalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
  }

  public ApplicationUpdate stateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
    return this;
  }

  /**
   * State.
   * @return stateProvinceId
   */
  @Valid 
  @JsonProperty("stateProvinceId")
  public @Nullable UUID getStateProvinceId() {
    return stateProvinceId;
  }

  @JsonProperty("stateProvinceId")
  public void setStateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
  }

  public ApplicationUpdate countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  @Valid 
  @JsonProperty("countryId")
  public @Nullable UUID getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable UUID countryId) {
    this.countryId = countryId;
  }

  public ApplicationUpdate permanentAddress(@Nullable String permanentAddress) {
    this.permanentAddress = permanentAddress;
    return this;
  }

  /**
   * Permanent address (free text).
   * @return permanentAddress
   */
  
  @JsonProperty("permanentAddress")
  public @Nullable String getPermanentAddress() {
    return permanentAddress;
  }

  @JsonProperty("permanentAddress")
  public void setPermanentAddress(@Nullable String permanentAddress) {
    this.permanentAddress = permanentAddress;
  }

  public ApplicationUpdate communicationAddress(@Nullable String communicationAddress) {
    this.communicationAddress = communicationAddress;
    return this;
  }

  /**
   * Address for communication (free text).
   * @return communicationAddress
   */
  
  @JsonProperty("communicationAddress")
  public @Nullable String getCommunicationAddress() {
    return communicationAddress;
  }

  @JsonProperty("communicationAddress")
  public void setCommunicationAddress(@Nullable String communicationAddress) {
    this.communicationAddress = communicationAddress;
  }

  public ApplicationUpdate previousInstitution(@Nullable String previousInstitution) {
    this.previousInstitution = previousInstitution;
    return this;
  }

  /**
   * Last school / institute attended.
   * @return previousInstitution
   */
  @Size(max = 255) 
  @JsonProperty("previousInstitution")
  public @Nullable String getPreviousInstitution() {
    return previousInstitution;
  }

  @JsonProperty("previousInstitution")
  public void setPreviousInstitution(@Nullable String previousInstitution) {
    this.previousInstitution = previousInstitution;
  }

  public ApplicationUpdate previousCourse(@Nullable String previousCourse) {
    this.previousCourse = previousCourse;
    return this;
  }

  /**
   * Last course / class studied.
   * @return previousCourse
   */
  @Size(max = 150) 
  @JsonProperty("previousCourse")
  public @Nullable String getPreviousCourse() {
    return previousCourse;
  }

  @JsonProperty("previousCourse")
  public void setPreviousCourse(@Nullable String previousCourse) {
    this.previousCourse = previousCourse;
  }

  public ApplicationUpdate previousResult(@Nullable String previousResult) {
    this.previousResult = previousResult;
    return this;
  }

  /**
   * Last result / marks.
   * @return previousResult
   */
  @Size(max = 255) 
  @JsonProperty("previousResult")
  public @Nullable String getPreviousResult() {
    return previousResult;
  }

  @JsonProperty("previousResult")
  public void setPreviousResult(@Nullable String previousResult) {
    this.previousResult = previousResult;
  }

  public ApplicationUpdate previousBoard(@Nullable PreviousBoardEnum previousBoard) {
    this.previousBoard = previousBoard;
    return this;
  }

  /**
   * Previous examination board.
   * @return previousBoard
   */
  
  @JsonProperty("previousBoard")
  public @Nullable PreviousBoardEnum getPreviousBoard() {
    return previousBoard;
  }

  @JsonProperty("previousBoard")
  public void setPreviousBoard(@Nullable PreviousBoardEnum previousBoard) {
    this.previousBoard = previousBoard;
  }

  public ApplicationUpdate boardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
    return this;
  }

  /**
   * Previous board registration number.
   * @return boardRegistrationNumber
   */
  @Size(max = 40) 
  @JsonProperty("boardRegistrationNumber")
  public @Nullable String getBoardRegistrationNumber() {
    return boardRegistrationNumber;
  }

  @JsonProperty("boardRegistrationNumber")
  public void setBoardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
  }

  public ApplicationUpdate reasonForLeaving(@Nullable String reasonForLeaving) {
    this.reasonForLeaving = reasonForLeaving;
    return this;
  }

  /**
   * Reason for leaving the previous institution.
   * @return reasonForLeaving
   */
  
  @JsonProperty("reasonForLeaving")
  public @Nullable String getReasonForLeaving() {
    return reasonForLeaving;
  }

  @JsonProperty("reasonForLeaving")
  public void setReasonForLeaving(@Nullable String reasonForLeaving) {
    this.reasonForLeaving = reasonForLeaving;
  }

  public ApplicationUpdate languageChoice(@Nullable String languageChoice) {
    this.languageChoice = languageChoice;
    return this;
  }

  /**
   * Second-language preference.
   * @return languageChoice
   */
  @Size(max = 60) 
  @JsonProperty("languageChoice")
  public @Nullable String getLanguageChoice() {
    return languageChoice;
  }

  @JsonProperty("languageChoice")
  public void setLanguageChoice(@Nullable String languageChoice) {
    this.languageChoice = languageChoice;
  }

  public ApplicationUpdate streamChoice(@Nullable String streamChoice) {
    this.streamChoice = streamChoice;
    return this;
  }

  /**
   * Subject group / stream preference.
   * @return streamChoice
   */
  
  @JsonProperty("streamChoice")
  public @Nullable String getStreamChoice() {
    return streamChoice;
  }

  @JsonProperty("streamChoice")
  public void setStreamChoice(@Nullable String streamChoice) {
    this.streamChoice = streamChoice;
  }

  public ApplicationUpdate hasSiblings(@Nullable Boolean hasSiblings) {
    this.hasSiblings = hasSiblings;
    return this;
  }

  /**
   * Has siblings in the institution.
   * @return hasSiblings
   */
  
  @JsonProperty("hasSiblings")
  public @Nullable Boolean getHasSiblings() {
    return hasSiblings;
  }

  @JsonProperty("hasSiblings")
  public void setHasSiblings(@Nullable Boolean hasSiblings) {
    this.hasSiblings = hasSiblings;
  }

  public ApplicationUpdate hasMedicalHistory(@Nullable Boolean hasMedicalHistory) {
    this.hasMedicalHistory = hasMedicalHistory;
    return this;
  }

  /**
   * Has a medical history to declare.
   * @return hasMedicalHistory
   */
  
  @JsonProperty("hasMedicalHistory")
  public @Nullable Boolean getHasMedicalHistory() {
    return hasMedicalHistory;
  }

  @JsonProperty("hasMedicalHistory")
  public void setHasMedicalHistory(@Nullable Boolean hasMedicalHistory) {
    this.hasMedicalHistory = hasMedicalHistory;
  }

  public ApplicationUpdate medicalDetails(@Nullable String medicalDetails) {
    this.medicalDetails = medicalDetails;
    return this;
  }

  /**
   * Medical details.
   * @return medicalDetails
   */
  
  @JsonProperty("medicalDetails")
  public @Nullable String getMedicalDetails() {
    return medicalDetails;
  }

  @JsonProperty("medicalDetails")
  public void setMedicalDetails(@Nullable String medicalDetails) {
    this.medicalDetails = medicalDetails;
  }

  public ApplicationUpdate hasExtracurricular(@Nullable Boolean hasExtracurricular) {
    this.hasExtracurricular = hasExtracurricular;
    return this;
  }

  /**
   * Has extracurricular activities.
   * @return hasExtracurricular
   */
  
  @JsonProperty("hasExtracurricular")
  public @Nullable Boolean getHasExtracurricular() {
    return hasExtracurricular;
  }

  @JsonProperty("hasExtracurricular")
  public void setHasExtracurricular(@Nullable Boolean hasExtracurricular) {
    this.hasExtracurricular = hasExtracurricular;
  }

  public ApplicationUpdate extracurricularDetails(@Nullable String extracurricularDetails) {
    this.extracurricularDetails = extracurricularDetails;
    return this;
  }

  /**
   * Activities.
   * @return extracurricularDetails
   */
  
  @JsonProperty("extracurricularDetails")
  public @Nullable String getExtracurricularDetails() {
    return extracurricularDetails;
  }

  @JsonProperty("extracurricularDetails")
  public void setExtracurricularDetails(@Nullable String extracurricularDetails) {
    this.extracurricularDetails = extracurricularDetails;
  }

  public ApplicationUpdate transportMode(@Nullable TransportModeEnum transportMode) {
    this.transportMode = transportMode;
    return this;
  }

  /**
   * Mode of transport.
   * @return transportMode
   */
  
  @JsonProperty("transportMode")
  public @Nullable TransportModeEnum getTransportMode() {
    return transportMode;
  }

  @JsonProperty("transportMode")
  public void setTransportMode(@Nullable TransportModeEnum transportMode) {
    this.transportMode = transportMode;
  }

  public ApplicationUpdate transportDetails(@Nullable String transportDetails) {
    this.transportDetails = transportDetails;
    return this;
  }

  /**
   * Transport details.
   * @return transportDetails
   */
  
  @JsonProperty("transportDetails")
  public @Nullable String getTransportDetails() {
    return transportDetails;
  }

  @JsonProperty("transportDetails")
  public void setTransportDetails(@Nullable String transportDetails) {
    this.transportDetails = transportDetails;
  }

  public ApplicationUpdate familyBusiness(@Nullable String familyBusiness) {
    this.familyBusiness = familyBusiness;
    return this;
  }

  /**
   * Family business.
   * @return familyBusiness
   */
  @Size(max = 150) 
  @JsonProperty("familyBusiness")
  public @Nullable String getFamilyBusiness() {
    return familyBusiness;
  }

  @JsonProperty("familyBusiness")
  public void setFamilyBusiness(@Nullable String familyBusiness) {
    this.familyBusiness = familyBusiness;
  }

  public ApplicationUpdate familyIncome(@Nullable String familyIncome) {
    this.familyIncome = familyIncome;
    return this;
  }

  /**
   * Family income.
   * @return familyIncome
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("familyIncome")
  public @Nullable String getFamilyIncome() {
    return familyIncome;
  }

  @JsonProperty("familyIncome")
  public void setFamilyIncome(@Nullable String familyIncome) {
    this.familyIncome = familyIncome;
  }

  public ApplicationUpdate applicationFee(@Nullable String applicationFee) {
    this.applicationFee = applicationFee;
    return this;
  }

  /**
   * Application / admission fee quoted.
   * @return applicationFee
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("applicationFee")
  public @Nullable String getApplicationFee() {
    return applicationFee;
  }

  @JsonProperty("applicationFee")
  public void setApplicationFee(@Nullable String applicationFee) {
    this.applicationFee = applicationFee;
  }

  public ApplicationUpdate discountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
    return this;
  }

  /**
   * Discount granted.
   * @return discountPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountPercent")
  public @Nullable String getDiscountPercent() {
    return discountPercent;
  }

  @JsonProperty("discountPercent")
  public void setDiscountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
  }

  public ApplicationUpdate feePlanId(@Nullable UUID feePlanId) {
    this.feePlanId = feePlanId;
    return this;
  }

  /**
   * Fee plan for the admission.
   * @return feePlanId
   */
  @Valid 
  @JsonProperty("feePlanId")
  public @Nullable UUID getFeePlanId() {
    return feePlanId;
  }

  @JsonProperty("feePlanId")
  public void setFeePlanId(@Nullable UUID feePlanId) {
    this.feePlanId = feePlanId;
  }

  public ApplicationUpdate feeStartDate(@Nullable LocalDate feeStartDate) {
    this.feeStartDate = feeStartDate;
    return this;
  }

  /**
   * Installment start date.
   * @return feeStartDate
   */
  @Valid 
  @JsonProperty("feeStartDate")
  public @Nullable LocalDate getFeeStartDate() {
    return feeStartDate;
  }

  @JsonProperty("feeStartDate")
  public void setFeeStartDate(@Nullable LocalDate feeStartDate) {
    this.feeStartDate = feeStartDate;
  }

  public ApplicationUpdate paymentDueDate(@Nullable LocalDate paymentDueDate) {
    this.paymentDueDate = paymentDueDate;
    return this;
  }

  /**
   * Payment due date.
   * @return paymentDueDate
   */
  @Valid 
  @JsonProperty("paymentDueDate")
  public @Nullable LocalDate getPaymentDueDate() {
    return paymentDueDate;
  }

  @JsonProperty("paymentDueDate")
  public void setPaymentDueDate(@Nullable LocalDate paymentDueDate) {
    this.paymentDueDate = paymentDueDate;
  }

  public ApplicationUpdate paymentStatus(@Nullable PaymentStatusEnum paymentStatus) {
    this.paymentStatus = paymentStatus;
    return this;
  }

  /**
   * Fee payment state.
   * @return paymentStatus
   */
  
  @JsonProperty("paymentStatus")
  public @Nullable PaymentStatusEnum getPaymentStatus() {
    return paymentStatus;
  }

  @JsonProperty("paymentStatus")
  public void setPaymentStatus(@Nullable PaymentStatusEnum paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public ApplicationUpdate remarks(@Nullable String remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Office remarks.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable String getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable String remarks) {
    this.remarks = remarks;
  }

  public ApplicationUpdate paymentStatusNote(@Nullable String paymentStatusNote) {
    this.paymentStatusNote = paymentStatusNote;
    return this;
  }

  /**
   * Free-text payment status from the payment desk when it does not fit payment_status.
   * @return paymentStatusNote
   */
  @Size(max = 120) 
  @JsonProperty("paymentStatusNote")
  public @Nullable String getPaymentStatusNote() {
    return paymentStatusNote;
  }

  @JsonProperty("paymentStatusNote")
  public void setPaymentStatusNote(@Nullable String paymentStatusNote) {
    this.paymentStatusNote = paymentStatusNote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationUpdate applicationUpdate = (ApplicationUpdate) o;
    return Objects.equals(this.institutionId, applicationUpdate.institutionId) &&
        Objects.equals(this.admissionCycleId, applicationUpdate.admissionCycleId) &&
        Objects.equals(this.applicationNumber, applicationUpdate.applicationNumber) &&
        Objects.equals(this.applicationDate, applicationUpdate.applicationDate) &&
        Objects.equals(this.admissionDate, applicationUpdate.admissionDate) &&
        Objects.equals(this.academicYearId, applicationUpdate.academicYearId) &&
        Objects.equals(this.gradeLevelId, applicationUpdate.gradeLevelId) &&
        Objects.equals(this.sectionId, applicationUpdate.sectionId) &&
        Objects.equals(this.programId, applicationUpdate.programId) &&
        Objects.equals(this.courseId, applicationUpdate.courseId) &&
        Objects.equals(this.batchId, applicationUpdate.batchId) &&
        Objects.equals(this.applicantPersonId, applicationUpdate.applicantPersonId) &&
        Objects.equals(this.studentId, applicationUpdate.studentId) &&
        Objects.equals(this.isExistingStudent, applicationUpdate.isExistingStudent) &&
        Objects.equals(this.honorificId, applicationUpdate.honorificId) &&
        Objects.equals(this.fullName, applicationUpdate.fullName) &&
        Objects.equals(this.firstName, applicationUpdate.firstName) &&
        Objects.equals(this.middleName, applicationUpdate.middleName) &&
        Objects.equals(this.lastName, applicationUpdate.lastName) &&
        Objects.equals(this.dateOfBirth, applicationUpdate.dateOfBirth) &&
        Objects.equals(this.gender, applicationUpdate.gender) &&
        Objects.equals(this.birthPlace, applicationUpdate.birthPlace) &&
        Objects.equals(this.nationality, applicationUpdate.nationality) &&
        Objects.equals(this.religion, applicationUpdate.religion) &&
        Objects.equals(this.socialCategory, applicationUpdate.socialCategory) &&
        Objects.equals(this.motherTongue, applicationUpdate.motherTongue) &&
        Objects.equals(this.identificationMarks, applicationUpdate.identificationMarks) &&
        Objects.equals(this.nationalId, applicationUpdate.nationalId) &&
        Objects.equals(this.bloodGroup, applicationUpdate.bloodGroup) &&
        Objects.equals(this.heightCm, applicationUpdate.heightCm) &&
        Objects.equals(this.weightKg, applicationUpdate.weightKg) &&
        Objects.equals(this.photoFileId, applicationUpdate.photoFileId) &&
        Objects.equals(this.email, applicationUpdate.email) &&
        Objects.equals(this.phone, applicationUpdate.phone) &&
        Objects.equals(this.mobile, applicationUpdate.mobile) &&
        Objects.equals(this.addressLine1, applicationUpdate.addressLine1) &&
        Objects.equals(this.addressLine2, applicationUpdate.addressLine2) &&
        Objects.equals(this.cityName, applicationUpdate.cityName) &&
        Objects.equals(this.postalCode, applicationUpdate.postalCode) &&
        Objects.equals(this.stateProvinceId, applicationUpdate.stateProvinceId) &&
        Objects.equals(this.countryId, applicationUpdate.countryId) &&
        Objects.equals(this.permanentAddress, applicationUpdate.permanentAddress) &&
        Objects.equals(this.communicationAddress, applicationUpdate.communicationAddress) &&
        Objects.equals(this.previousInstitution, applicationUpdate.previousInstitution) &&
        Objects.equals(this.previousCourse, applicationUpdate.previousCourse) &&
        Objects.equals(this.previousResult, applicationUpdate.previousResult) &&
        Objects.equals(this.previousBoard, applicationUpdate.previousBoard) &&
        Objects.equals(this.boardRegistrationNumber, applicationUpdate.boardRegistrationNumber) &&
        Objects.equals(this.reasonForLeaving, applicationUpdate.reasonForLeaving) &&
        Objects.equals(this.languageChoice, applicationUpdate.languageChoice) &&
        Objects.equals(this.streamChoice, applicationUpdate.streamChoice) &&
        Objects.equals(this.hasSiblings, applicationUpdate.hasSiblings) &&
        Objects.equals(this.hasMedicalHistory, applicationUpdate.hasMedicalHistory) &&
        Objects.equals(this.medicalDetails, applicationUpdate.medicalDetails) &&
        Objects.equals(this.hasExtracurricular, applicationUpdate.hasExtracurricular) &&
        Objects.equals(this.extracurricularDetails, applicationUpdate.extracurricularDetails) &&
        Objects.equals(this.transportMode, applicationUpdate.transportMode) &&
        Objects.equals(this.transportDetails, applicationUpdate.transportDetails) &&
        Objects.equals(this.familyBusiness, applicationUpdate.familyBusiness) &&
        Objects.equals(this.familyIncome, applicationUpdate.familyIncome) &&
        Objects.equals(this.applicationFee, applicationUpdate.applicationFee) &&
        Objects.equals(this.discountPercent, applicationUpdate.discountPercent) &&
        Objects.equals(this.feePlanId, applicationUpdate.feePlanId) &&
        Objects.equals(this.feeStartDate, applicationUpdate.feeStartDate) &&
        Objects.equals(this.paymentDueDate, applicationUpdate.paymentDueDate) &&
        Objects.equals(this.paymentStatus, applicationUpdate.paymentStatus) &&
        Objects.equals(this.remarks, applicationUpdate.remarks) &&
        Objects.equals(this.paymentStatusNote, applicationUpdate.paymentStatusNote);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, admissionCycleId, applicationNumber, applicationDate, admissionDate, academicYearId, gradeLevelId, sectionId, programId, courseId, batchId, applicantPersonId, studentId, isExistingStudent, honorificId, fullName, firstName, middleName, lastName, dateOfBirth, gender, birthPlace, nationality, religion, socialCategory, motherTongue, identificationMarks, nationalId, bloodGroup, heightCm, weightKg, photoFileId, email, phone, mobile, addressLine1, addressLine2, cityName, postalCode, stateProvinceId, countryId, permanentAddress, communicationAddress, previousInstitution, previousCourse, previousResult, previousBoard, boardRegistrationNumber, reasonForLeaving, languageChoice, streamChoice, hasSiblings, hasMedicalHistory, medicalDetails, hasExtracurricular, extracurricularDetails, transportMode, transportDetails, familyBusiness, familyIncome, applicationFee, discountPercent, feePlanId, feeStartDate, paymentDueDate, paymentStatus, remarks, paymentStatusNote);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    admissionCycleId: ").append(toIndentedString(admissionCycleId)).append("\n");
    sb.append("    applicationNumber: ").append(toIndentedString(applicationNumber)).append("\n");
    sb.append("    applicationDate: ").append(toIndentedString(applicationDate)).append("\n");
    sb.append("    admissionDate: ").append(toIndentedString(admissionDate)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    gradeLevelId: ").append(toIndentedString(gradeLevelId)).append("\n");
    sb.append("    sectionId: ").append(toIndentedString(sectionId)).append("\n");
    sb.append("    programId: ").append(toIndentedString(programId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    applicantPersonId: ").append(toIndentedString(applicantPersonId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    isExistingStudent: ").append(toIndentedString(isExistingStudent)).append("\n");
    sb.append("    honorificId: ").append(toIndentedString(honorificId)).append("\n");
    sb.append("    fullName: ").append(toIndentedString(fullName)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    middleName: ").append(toIndentedString(middleName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    birthPlace: ").append(toIndentedString(birthPlace)).append("\n");
    sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
    sb.append("    religion: ").append(toIndentedString(religion)).append("\n");
    sb.append("    socialCategory: ").append(toIndentedString(socialCategory)).append("\n");
    sb.append("    motherTongue: ").append(toIndentedString(motherTongue)).append("\n");
    sb.append("    identificationMarks: ").append(toIndentedString(identificationMarks)).append("\n");
    sb.append("    nationalId: ").append(toIndentedString(nationalId)).append("\n");
    sb.append("    bloodGroup: ").append(toIndentedString(bloodGroup)).append("\n");
    sb.append("    heightCm: ").append(toIndentedString(heightCm)).append("\n");
    sb.append("    weightKg: ").append(toIndentedString(weightKg)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    mobile: ").append(toIndentedString(mobile)).append("\n");
    sb.append("    addressLine1: ").append(toIndentedString(addressLine1)).append("\n");
    sb.append("    addressLine2: ").append(toIndentedString(addressLine2)).append("\n");
    sb.append("    cityName: ").append(toIndentedString(cityName)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    stateProvinceId: ").append(toIndentedString(stateProvinceId)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    permanentAddress: ").append(toIndentedString(permanentAddress)).append("\n");
    sb.append("    communicationAddress: ").append(toIndentedString(communicationAddress)).append("\n");
    sb.append("    previousInstitution: ").append(toIndentedString(previousInstitution)).append("\n");
    sb.append("    previousCourse: ").append(toIndentedString(previousCourse)).append("\n");
    sb.append("    previousResult: ").append(toIndentedString(previousResult)).append("\n");
    sb.append("    previousBoard: ").append(toIndentedString(previousBoard)).append("\n");
    sb.append("    boardRegistrationNumber: ").append(toIndentedString(boardRegistrationNumber)).append("\n");
    sb.append("    reasonForLeaving: ").append(toIndentedString(reasonForLeaving)).append("\n");
    sb.append("    languageChoice: ").append(toIndentedString(languageChoice)).append("\n");
    sb.append("    streamChoice: ").append(toIndentedString(streamChoice)).append("\n");
    sb.append("    hasSiblings: ").append(toIndentedString(hasSiblings)).append("\n");
    sb.append("    hasMedicalHistory: ").append(toIndentedString(hasMedicalHistory)).append("\n");
    sb.append("    medicalDetails: ").append(toIndentedString(medicalDetails)).append("\n");
    sb.append("    hasExtracurricular: ").append(toIndentedString(hasExtracurricular)).append("\n");
    sb.append("    extracurricularDetails: ").append(toIndentedString(extracurricularDetails)).append("\n");
    sb.append("    transportMode: ").append(toIndentedString(transportMode)).append("\n");
    sb.append("    transportDetails: ").append(toIndentedString(transportDetails)).append("\n");
    sb.append("    familyBusiness: ").append(toIndentedString(familyBusiness)).append("\n");
    sb.append("    familyIncome: ").append(toIndentedString(familyIncome)).append("\n");
    sb.append("    applicationFee: ").append(toIndentedString(applicationFee)).append("\n");
    sb.append("    discountPercent: ").append(toIndentedString(discountPercent)).append("\n");
    sb.append("    feePlanId: ").append(toIndentedString(feePlanId)).append("\n");
    sb.append("    feeStartDate: ").append(toIndentedString(feeStartDate)).append("\n");
    sb.append("    paymentDueDate: ").append(toIndentedString(paymentDueDate)).append("\n");
    sb.append("    paymentStatus: ").append(toIndentedString(paymentStatus)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
    sb.append("    paymentStatusNote: ").append(toIndentedString(paymentStatusNote)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

