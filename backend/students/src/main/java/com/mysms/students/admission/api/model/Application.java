// Owned API contract of admission.
package com.mysms.students.admission.api.model;

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
 * Admission application with applicant details.
 */

public class Application {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID admissionCycleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String applicationNumber;

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
  private String fullName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String firstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String middleName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String lastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateOfBirth;

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
  private GenderEnum gender;

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
  private @Nullable String nationalIdMasked;

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

  /**
   * Application state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    SUBMITTED("submitted"),
    
    PENDING("pending"),
    
    CONFIRMED("confirmed"),
    
    APPROVED("approved"),
    
    REJECTED("rejected"),
    
    ADMITTED("admitted"),
    
    CANCELLED("cancelled"),
    
    DONE("done");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String paymentStatusNote;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Application() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Application(UUID institutionId, String applicationNumber, String fullName, LocalDate dateOfBirth, GenderEnum gender) {
    this.institutionId = institutionId;
    this.applicationNumber = applicationNumber;
    this.fullName = fullName;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
  }

  public Application id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Application institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public Application admissionCycleId(@Nullable UUID admissionCycleId) {
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

  public Application applicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
    return this;
  }

  /**
   * Application number.
   * @return applicationNumber
   */
  @NotNull @Size(max = 30) 
  @JsonProperty("applicationNumber")
  public String getApplicationNumber() {
    return applicationNumber;
  }

  @JsonProperty("applicationNumber")
  public void setApplicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public Application applicationDate(@Nullable OffsetDateTime applicationDate) {
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

  public Application admissionDate(@Nullable LocalDate admissionDate) {
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

  public Application academicYearId(@Nullable UUID academicYearId) {
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

  public Application gradeLevelId(@Nullable UUID gradeLevelId) {
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

  public Application sectionId(@Nullable UUID sectionId) {
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

  public Application programId(@Nullable UUID programId) {
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

  public Application courseId(@Nullable UUID courseId) {
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

  public Application batchId(@Nullable UUID batchId) {
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

  public Application applicantPersonId(@Nullable UUID applicantPersonId) {
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

  public Application studentId(@Nullable UUID studentId) {
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

  public Application isExistingStudent(@Nullable Boolean isExistingStudent) {
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

  public Application honorificId(@Nullable UUID honorificId) {
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

  public Application fullName(String fullName) {
    this.fullName = fullName;
    return this;
  }

  /**
   * Applicant full name.
   * @return fullName
   */
  @NotNull @Size(max = 255) 
  @JsonProperty("fullName")
  public String getFullName() {
    return fullName;
  }

  @JsonProperty("fullName")
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public Application firstName(@Nullable String firstName) {
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

  public Application middleName(@Nullable String middleName) {
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

  public Application lastName(@Nullable String lastName) {
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

  public Application dateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  /**
   * Date of birth.
   * @return dateOfBirth
   */
  @NotNull @Valid 
  @JsonProperty("dateOfBirth")
  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Application gender(GenderEnum gender) {
    this.gender = gender;
    return this;
  }

  /**
   * Gender.
   * @return gender
   */
  @NotNull 
  @JsonProperty("gender")
  public GenderEnum getGender() {
    return gender;
  }

  @JsonProperty("gender")
  public void setGender(GenderEnum gender) {
    this.gender = gender;
  }

  public Application birthPlace(@Nullable String birthPlace) {
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

  public Application nationality(@Nullable String nationality) {
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

  public Application religion(@Nullable String religion) {
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

  public Application socialCategory(@Nullable String socialCategory) {
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

  public Application motherTongue(@Nullable String motherTongue) {
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

  public Application identificationMarks(@Nullable String identificationMarks) {
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

  public Application nationalId(@Nullable String nationalId) {
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

  public Application nationalIdMasked(@Nullable String nationalIdMasked) {
    this.nationalIdMasked = nationalIdMasked;
    return this;
  }

  /**
   * Masked form for display, e.g. \"••••1234\". Output only.
   * @return nationalIdMasked
   */
  
  @JsonProperty("nationalIdMasked")
  public @Nullable String getNationalIdMasked() {
    return nationalIdMasked;
  }

  @JsonProperty("nationalIdMasked")
  public void setNationalIdMasked(@Nullable String nationalIdMasked) {
    this.nationalIdMasked = nationalIdMasked;
  }

  public Application bloodGroup(@Nullable String bloodGroup) {
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

  public Application heightCm(@Nullable String heightCm) {
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

  public Application weightKg(@Nullable String weightKg) {
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

  public Application photoFileId(@Nullable UUID photoFileId) {
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

  public Application email(@Nullable String email) {
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

  public Application phone(@Nullable String phone) {
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

  public Application mobile(@Nullable String mobile) {
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

  public Application addressLine1(@Nullable String addressLine1) {
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

  public Application addressLine2(@Nullable String addressLine2) {
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

  public Application cityName(@Nullable String cityName) {
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

  public Application postalCode(@Nullable String postalCode) {
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

  public Application stateProvinceId(@Nullable UUID stateProvinceId) {
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

  public Application countryId(@Nullable UUID countryId) {
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

  public Application permanentAddress(@Nullable String permanentAddress) {
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

  public Application communicationAddress(@Nullable String communicationAddress) {
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

  public Application previousInstitution(@Nullable String previousInstitution) {
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

  public Application previousCourse(@Nullable String previousCourse) {
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

  public Application previousResult(@Nullable String previousResult) {
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

  public Application previousBoard(@Nullable PreviousBoardEnum previousBoard) {
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

  public Application boardRegistrationNumber(@Nullable String boardRegistrationNumber) {
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

  public Application reasonForLeaving(@Nullable String reasonForLeaving) {
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

  public Application languageChoice(@Nullable String languageChoice) {
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

  public Application streamChoice(@Nullable String streamChoice) {
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

  public Application hasSiblings(@Nullable Boolean hasSiblings) {
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

  public Application hasMedicalHistory(@Nullable Boolean hasMedicalHistory) {
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

  public Application medicalDetails(@Nullable String medicalDetails) {
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

  public Application hasExtracurricular(@Nullable Boolean hasExtracurricular) {
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

  public Application extracurricularDetails(@Nullable String extracurricularDetails) {
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

  public Application transportMode(@Nullable TransportModeEnum transportMode) {
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

  public Application transportDetails(@Nullable String transportDetails) {
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

  public Application familyBusiness(@Nullable String familyBusiness) {
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

  public Application familyIncome(@Nullable String familyIncome) {
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

  public Application applicationFee(@Nullable String applicationFee) {
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

  public Application discountPercent(@Nullable String discountPercent) {
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

  public Application feePlanId(@Nullable UUID feePlanId) {
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

  public Application feeStartDate(@Nullable LocalDate feeStartDate) {
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

  public Application paymentDueDate(@Nullable LocalDate paymentDueDate) {
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

  public Application paymentStatus(@Nullable PaymentStatusEnum paymentStatus) {
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

  public Application status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Application state. Changed only through the workflow operations.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public Application remarks(@Nullable String remarks) {
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

  public Application paymentStatusNote(@Nullable String paymentStatusNote) {
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

  public Application createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Application createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Application updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Application updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Application etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Application application = (Application) o;
    return Objects.equals(this.id, application.id) &&
        Objects.equals(this.institutionId, application.institutionId) &&
        Objects.equals(this.admissionCycleId, application.admissionCycleId) &&
        Objects.equals(this.applicationNumber, application.applicationNumber) &&
        Objects.equals(this.applicationDate, application.applicationDate) &&
        Objects.equals(this.admissionDate, application.admissionDate) &&
        Objects.equals(this.academicYearId, application.academicYearId) &&
        Objects.equals(this.gradeLevelId, application.gradeLevelId) &&
        Objects.equals(this.sectionId, application.sectionId) &&
        Objects.equals(this.programId, application.programId) &&
        Objects.equals(this.courseId, application.courseId) &&
        Objects.equals(this.batchId, application.batchId) &&
        Objects.equals(this.applicantPersonId, application.applicantPersonId) &&
        Objects.equals(this.studentId, application.studentId) &&
        Objects.equals(this.isExistingStudent, application.isExistingStudent) &&
        Objects.equals(this.honorificId, application.honorificId) &&
        Objects.equals(this.fullName, application.fullName) &&
        Objects.equals(this.firstName, application.firstName) &&
        Objects.equals(this.middleName, application.middleName) &&
        Objects.equals(this.lastName, application.lastName) &&
        Objects.equals(this.dateOfBirth, application.dateOfBirth) &&
        Objects.equals(this.gender, application.gender) &&
        Objects.equals(this.birthPlace, application.birthPlace) &&
        Objects.equals(this.nationality, application.nationality) &&
        Objects.equals(this.religion, application.religion) &&
        Objects.equals(this.socialCategory, application.socialCategory) &&
        Objects.equals(this.motherTongue, application.motherTongue) &&
        Objects.equals(this.identificationMarks, application.identificationMarks) &&
        Objects.equals(this.nationalId, application.nationalId) &&
        Objects.equals(this.nationalIdMasked, application.nationalIdMasked) &&
        Objects.equals(this.bloodGroup, application.bloodGroup) &&
        Objects.equals(this.heightCm, application.heightCm) &&
        Objects.equals(this.weightKg, application.weightKg) &&
        Objects.equals(this.photoFileId, application.photoFileId) &&
        Objects.equals(this.email, application.email) &&
        Objects.equals(this.phone, application.phone) &&
        Objects.equals(this.mobile, application.mobile) &&
        Objects.equals(this.addressLine1, application.addressLine1) &&
        Objects.equals(this.addressLine2, application.addressLine2) &&
        Objects.equals(this.cityName, application.cityName) &&
        Objects.equals(this.postalCode, application.postalCode) &&
        Objects.equals(this.stateProvinceId, application.stateProvinceId) &&
        Objects.equals(this.countryId, application.countryId) &&
        Objects.equals(this.permanentAddress, application.permanentAddress) &&
        Objects.equals(this.communicationAddress, application.communicationAddress) &&
        Objects.equals(this.previousInstitution, application.previousInstitution) &&
        Objects.equals(this.previousCourse, application.previousCourse) &&
        Objects.equals(this.previousResult, application.previousResult) &&
        Objects.equals(this.previousBoard, application.previousBoard) &&
        Objects.equals(this.boardRegistrationNumber, application.boardRegistrationNumber) &&
        Objects.equals(this.reasonForLeaving, application.reasonForLeaving) &&
        Objects.equals(this.languageChoice, application.languageChoice) &&
        Objects.equals(this.streamChoice, application.streamChoice) &&
        Objects.equals(this.hasSiblings, application.hasSiblings) &&
        Objects.equals(this.hasMedicalHistory, application.hasMedicalHistory) &&
        Objects.equals(this.medicalDetails, application.medicalDetails) &&
        Objects.equals(this.hasExtracurricular, application.hasExtracurricular) &&
        Objects.equals(this.extracurricularDetails, application.extracurricularDetails) &&
        Objects.equals(this.transportMode, application.transportMode) &&
        Objects.equals(this.transportDetails, application.transportDetails) &&
        Objects.equals(this.familyBusiness, application.familyBusiness) &&
        Objects.equals(this.familyIncome, application.familyIncome) &&
        Objects.equals(this.applicationFee, application.applicationFee) &&
        Objects.equals(this.discountPercent, application.discountPercent) &&
        Objects.equals(this.feePlanId, application.feePlanId) &&
        Objects.equals(this.feeStartDate, application.feeStartDate) &&
        Objects.equals(this.paymentDueDate, application.paymentDueDate) &&
        Objects.equals(this.paymentStatus, application.paymentStatus) &&
        Objects.equals(this.status, application.status) &&
        Objects.equals(this.remarks, application.remarks) &&
        Objects.equals(this.paymentStatusNote, application.paymentStatusNote) &&
        Objects.equals(this.createTime, application.createTime) &&
        Objects.equals(this.createdBy, application.createdBy) &&
        Objects.equals(this.updateTime, application.updateTime) &&
        Objects.equals(this.updatedBy, application.updatedBy) &&
        Objects.equals(this.etag, application.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, institutionId, admissionCycleId, applicationNumber, applicationDate, admissionDate, academicYearId, gradeLevelId, sectionId, programId, courseId, batchId, applicantPersonId, studentId, isExistingStudent, honorificId, fullName, firstName, middleName, lastName, dateOfBirth, gender, birthPlace, nationality, religion, socialCategory, motherTongue, identificationMarks, nationalId, nationalIdMasked, bloodGroup, heightCm, weightKg, photoFileId, email, phone, mobile, addressLine1, addressLine2, cityName, postalCode, stateProvinceId, countryId, permanentAddress, communicationAddress, previousInstitution, previousCourse, previousResult, previousBoard, boardRegistrationNumber, reasonForLeaving, languageChoice, streamChoice, hasSiblings, hasMedicalHistory, medicalDetails, hasExtracurricular, extracurricularDetails, transportMode, transportDetails, familyBusiness, familyIncome, applicationFee, discountPercent, feePlanId, feeStartDate, paymentDueDate, paymentStatus, status, remarks, paymentStatusNote, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Application {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    nationalIdMasked: ").append(toIndentedString(nationalIdMasked)).append("\n");
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
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
    sb.append("    paymentStatusNote: ").append(toIndentedString(paymentStatusNote)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

