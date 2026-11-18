// Owned API contract of people.
package com.mysms.people.api.model;

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
 * A human being. Shared identity for student, guardian, staff, applicant and student roles.
 */

public class Person {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID honorificId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String firstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String middleName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String lastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateOfBirth;

  /**
   * Gender.
   */
  public enum GenderEnum {
    MALE("male"),
    
    FEMALE("female"),
    
    OTHER("other"),
    
    UNDISCLOSED("undisclosed");

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

  /**
   * Blood group.
   */
  public enum BloodGroupEnum {
    A_("A+"),
    
    A_2("A-"),
    
    B_("B+"),
    
    B_2("B-"),
    
    O_("O+"),
    
    O_2("O-"),
    
    AB_("AB+"),
    
    AB_2("AB-"),
    
    A1_("A1+"),
    
    A1_2("A1-"),
    
    B1_("B1+"),
    
    B1_2("B1-"),
    
    A1_B_("A1B+"),
    
    A1_B_2("A1B-");

    private final String value;

    BloodGroupEnum(String value) {
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
    public static BloodGroupEnum fromValue(String value) {
      for (BloodGroupEnum b : BloodGroupEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable BloodGroupEnum bloodGroup;

  /**
   * Marital status.
   */
  public enum MaritalStatusEnum {
    SINGLE("single"),
    
    MARRIED("married"),
    
    DIVORCED("divorced"),
    
    WIDOWED("widowed");

    private final String value;

    MaritalStatusEnum(String value) {
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
    public static MaritalStatusEnum fromValue(String value) {
      for (MaritalStatusEnum b : MaritalStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MaritalStatusEnum maritalStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String birthPlace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nativePlace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID nationalityCountryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String motherTongue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String religion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String socialCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String socialSubCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationalIdType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationalId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationalIdMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String visaInfo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String primaryEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String primaryPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String alternatePhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String workPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String websiteUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String aboutMe;

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

  public Person() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Person(String displayName) {
    this.displayName = displayName;
  }

  public Person id(@Nullable UUID id) {
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

  public Person honorificId(@Nullable UUID honorificId) {
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

  public Person firstName(@Nullable String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Given name.
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

  public Person middleName(@Nullable String middleName) {
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

  public Person lastName(@Nullable String lastName) {
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

  public Person displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Full name as displayed.
   * @return displayName
   */
  @NotNull @Size(max = 255) 
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Person dateOfBirth(@Nullable LocalDate dateOfBirth) {
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

  public Person gender(@Nullable GenderEnum gender) {
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

  public Person bloodGroup(@Nullable BloodGroupEnum bloodGroup) {
    this.bloodGroup = bloodGroup;
    return this;
  }

  /**
   * Blood group.
   * @return bloodGroup
   */
  
  @JsonProperty("bloodGroup")
  public @Nullable BloodGroupEnum getBloodGroup() {
    return bloodGroup;
  }

  @JsonProperty("bloodGroup")
  public void setBloodGroup(@Nullable BloodGroupEnum bloodGroup) {
    this.bloodGroup = bloodGroup;
  }

  public Person maritalStatus(@Nullable MaritalStatusEnum maritalStatus) {
    this.maritalStatus = maritalStatus;
    return this;
  }

  /**
   * Marital status.
   * @return maritalStatus
   */
  
  @JsonProperty("maritalStatus")
  public @Nullable MaritalStatusEnum getMaritalStatus() {
    return maritalStatus;
  }

  @JsonProperty("maritalStatus")
  public void setMaritalStatus(@Nullable MaritalStatusEnum maritalStatus) {
    this.maritalStatus = maritalStatus;
  }

  public Person birthPlace(@Nullable String birthPlace) {
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

  public Person nativePlace(@Nullable String nativePlace) {
    this.nativePlace = nativePlace;
    return this;
  }

  /**
   * Native place / hometown.
   * @return nativePlace
   */
  @Size(max = 120) 
  @JsonProperty("nativePlace")
  public @Nullable String getNativePlace() {
    return nativePlace;
  }

  @JsonProperty("nativePlace")
  public void setNativePlace(@Nullable String nativePlace) {
    this.nativePlace = nativePlace;
  }

  public Person nationalityCountryId(@Nullable UUID nationalityCountryId) {
    this.nationalityCountryId = nationalityCountryId;
    return this;
  }

  /**
   * Nationality.
   * @return nationalityCountryId
   */
  @Valid 
  @JsonProperty("nationalityCountryId")
  public @Nullable UUID getNationalityCountryId() {
    return nationalityCountryId;
  }

  @JsonProperty("nationalityCountryId")
  public void setNationalityCountryId(@Nullable UUID nationalityCountryId) {
    this.nationalityCountryId = nationalityCountryId;
  }

  public Person motherTongue(@Nullable String motherTongue) {
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

  public Person religion(@Nullable String religion) {
    this.religion = religion;
    return this;
  }

  /**
   * Religion (optional, for statutory reporting).
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

  public Person socialCategory(@Nullable String socialCategory) {
    this.socialCategory = socialCategory;
    return this;
  }

  /**
   * Government social category used for statutory reporting (e.g. GEN, OBC, SC, ST).
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

  public Person socialSubCategory(@Nullable String socialSubCategory) {
    this.socialSubCategory = socialSubCategory;
    return this;
  }

  /**
   * Sub-category.
   * @return socialSubCategory
   */
  @Size(max = 60) 
  @JsonProperty("socialSubCategory")
  public @Nullable String getSocialSubCategory() {
    return socialSubCategory;
  }

  @JsonProperty("socialSubCategory")
  public void setSocialSubCategory(@Nullable String socialSubCategory) {
    this.socialSubCategory = socialSubCategory;
  }

  public Person nationalIdType(@Nullable String nationalIdType) {
    this.nationalIdType = nationalIdType;
    return this;
  }

  /**
   * Type of national id (e.g. AADHAAR, SSN, PASSPORT).
   * @return nationalIdType
   */
  @Size(max = 30) 
  @JsonProperty("nationalIdType")
  public @Nullable String getNationalIdType() {
    return nationalIdType;
  }

  @JsonProperty("nationalIdType")
  public void setNationalIdType(@Nullable String nationalIdType) {
    this.nationalIdType = nationalIdType;
  }

  public Person nationalId(@Nullable String nationalId) {
    this.nationalId = nationalId;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. National id number, encrypted at rest.
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

  public Person nationalIdMasked(@Nullable String nationalIdMasked) {
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

  public Person visaInfo(@Nullable String visaInfo) {
    this.visaInfo = visaInfo;
    return this;
  }

  /**
   * Visa / residence-permit details.
   * @return visaInfo
   */
  @Size(max = 100) 
  @JsonProperty("visaInfo")
  public @Nullable String getVisaInfo() {
    return visaInfo;
  }

  @JsonProperty("visaInfo")
  public void setVisaInfo(@Nullable String visaInfo) {
    this.visaInfo = visaInfo;
  }

  public Person primaryEmail(@Nullable String primaryEmail) {
    this.primaryEmail = primaryEmail;
    return this;
  }

  /**
   * Personal e-mail.
   * @return primaryEmail
   */
  @Size(max = 320) 
  @JsonProperty("primaryEmail")
  public @Nullable String getPrimaryEmail() {
    return primaryEmail;
  }

  @JsonProperty("primaryEmail")
  public void setPrimaryEmail(@Nullable String primaryEmail) {
    this.primaryEmail = primaryEmail;
  }

  public Person primaryPhone(@Nullable String primaryPhone) {
    this.primaryPhone = primaryPhone;
    return this;
  }

  /**
   * Personal phone.
   * @return primaryPhone
   */
  @Size(max = 20) 
  @JsonProperty("primaryPhone")
  public @Nullable String getPrimaryPhone() {
    return primaryPhone;
  }

  @JsonProperty("primaryPhone")
  public void setPrimaryPhone(@Nullable String primaryPhone) {
    this.primaryPhone = primaryPhone;
  }

  public Person alternatePhone(@Nullable String alternatePhone) {
    this.alternatePhone = alternatePhone;
    return this;
  }

  /**
   * Alternate phone.
   * @return alternatePhone
   */
  @Size(max = 20) 
  @JsonProperty("alternatePhone")
  public @Nullable String getAlternatePhone() {
    return alternatePhone;
  }

  @JsonProperty("alternatePhone")
  public void setAlternatePhone(@Nullable String alternatePhone) {
    this.alternatePhone = alternatePhone;
  }

  public Person photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Profile photo.
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

  public Person remarks(@Nullable String remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Free-text notes about the person.
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

  public Person workPhone(@Nullable String workPhone) {
    this.workPhone = workPhone;
    return this;
  }

  /**
   * Office / work phone.
   * @return workPhone
   */
  @Size(max = 30) 
  @JsonProperty("workPhone")
  public @Nullable String getWorkPhone() {
    return workPhone;
  }

  @JsonProperty("workPhone")
  public void setWorkPhone(@Nullable String workPhone) {
    this.workPhone = workPhone;
  }

  public Person websiteUrl(@Nullable String websiteUrl) {
    this.websiteUrl = websiteUrl;
    return this;
  }

  /**
   * Personal web page.
   * @return websiteUrl
   */
  @Size(max = 500) 
  @JsonProperty("websiteUrl")
  public @Nullable String getWebsiteUrl() {
    return websiteUrl;
  }

  @JsonProperty("websiteUrl")
  public void setWebsiteUrl(@Nullable String websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public Person aboutMe(@Nullable String aboutMe) {
    this.aboutMe = aboutMe;
    return this;
  }

  /**
   * Short self-description shown on the profile.
   * @return aboutMe
   */
  
  @JsonProperty("aboutMe")
  public @Nullable String getAboutMe() {
    return aboutMe;
  }

  @JsonProperty("aboutMe")
  public void setAboutMe(@Nullable String aboutMe) {
    this.aboutMe = aboutMe;
  }

  public Person createTime(@Nullable OffsetDateTime createTime) {
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

  public Person createdBy(@Nullable UUID createdBy) {
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

  public Person updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Person updatedBy(@Nullable UUID updatedBy) {
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

  public Person etag(@Nullable String etag) {
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
    Person person = (Person) o;
    return Objects.equals(this.id, person.id) &&
        Objects.equals(this.honorificId, person.honorificId) &&
        Objects.equals(this.firstName, person.firstName) &&
        Objects.equals(this.middleName, person.middleName) &&
        Objects.equals(this.lastName, person.lastName) &&
        Objects.equals(this.displayName, person.displayName) &&
        Objects.equals(this.dateOfBirth, person.dateOfBirth) &&
        Objects.equals(this.gender, person.gender) &&
        Objects.equals(this.bloodGroup, person.bloodGroup) &&
        Objects.equals(this.maritalStatus, person.maritalStatus) &&
        Objects.equals(this.birthPlace, person.birthPlace) &&
        Objects.equals(this.nativePlace, person.nativePlace) &&
        Objects.equals(this.nationalityCountryId, person.nationalityCountryId) &&
        Objects.equals(this.motherTongue, person.motherTongue) &&
        Objects.equals(this.religion, person.religion) &&
        Objects.equals(this.socialCategory, person.socialCategory) &&
        Objects.equals(this.socialSubCategory, person.socialSubCategory) &&
        Objects.equals(this.nationalIdType, person.nationalIdType) &&
        Objects.equals(this.nationalId, person.nationalId) &&
        Objects.equals(this.nationalIdMasked, person.nationalIdMasked) &&
        Objects.equals(this.visaInfo, person.visaInfo) &&
        Objects.equals(this.primaryEmail, person.primaryEmail) &&
        Objects.equals(this.primaryPhone, person.primaryPhone) &&
        Objects.equals(this.alternatePhone, person.alternatePhone) &&
        Objects.equals(this.photoFileId, person.photoFileId) &&
        Objects.equals(this.remarks, person.remarks) &&
        Objects.equals(this.workPhone, person.workPhone) &&
        Objects.equals(this.websiteUrl, person.websiteUrl) &&
        Objects.equals(this.aboutMe, person.aboutMe) &&
        Objects.equals(this.createTime, person.createTime) &&
        Objects.equals(this.createdBy, person.createdBy) &&
        Objects.equals(this.updateTime, person.updateTime) &&
        Objects.equals(this.updatedBy, person.updatedBy) &&
        Objects.equals(this.etag, person.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, honorificId, firstName, middleName, lastName, displayName, dateOfBirth, gender, bloodGroup, maritalStatus, birthPlace, nativePlace, nationalityCountryId, motherTongue, religion, socialCategory, socialSubCategory, nationalIdType, nationalId, nationalIdMasked, visaInfo, primaryEmail, primaryPhone, alternatePhone, photoFileId, remarks, workPhone, websiteUrl, aboutMe, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Person {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    honorificId: ").append(toIndentedString(honorificId)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    middleName: ").append(toIndentedString(middleName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    bloodGroup: ").append(toIndentedString(bloodGroup)).append("\n");
    sb.append("    maritalStatus: ").append(toIndentedString(maritalStatus)).append("\n");
    sb.append("    birthPlace: ").append(toIndentedString(birthPlace)).append("\n");
    sb.append("    nativePlace: ").append(toIndentedString(nativePlace)).append("\n");
    sb.append("    nationalityCountryId: ").append(toIndentedString(nationalityCountryId)).append("\n");
    sb.append("    motherTongue: ").append(toIndentedString(motherTongue)).append("\n");
    sb.append("    religion: ").append(toIndentedString(religion)).append("\n");
    sb.append("    socialCategory: ").append(toIndentedString(socialCategory)).append("\n");
    sb.append("    socialSubCategory: ").append(toIndentedString(socialSubCategory)).append("\n");
    sb.append("    nationalIdType: ").append(toIndentedString(nationalIdType)).append("\n");
    sb.append("    nationalId: ").append(toIndentedString(nationalId)).append("\n");
    sb.append("    nationalIdMasked: ").append(toIndentedString(nationalIdMasked)).append("\n");
    sb.append("    visaInfo: ").append(toIndentedString(visaInfo)).append("\n");
    sb.append("    primaryEmail: ").append(toIndentedString(primaryEmail)).append("\n");
    sb.append("    primaryPhone: ").append(toIndentedString(primaryPhone)).append("\n");
    sb.append("    alternatePhone: ").append(toIndentedString(alternatePhone)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
    sb.append("    workPhone: ").append(toIndentedString(workPhone)).append("\n");
    sb.append("    websiteUrl: ").append(toIndentedString(websiteUrl)).append("\n");
    sb.append("    aboutMe: ").append(toIndentedString(aboutMe)).append("\n");
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

