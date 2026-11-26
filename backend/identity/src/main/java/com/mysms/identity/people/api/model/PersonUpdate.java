// Owned API contract of people.
package com.mysms.identity.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Person that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PersonUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> honorificId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> firstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> middleName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> lastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> dateOfBirth;

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
  private @Nullable Optional<GenderEnum> gender;

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
  private @Nullable Optional<BloodGroupEnum> bloodGroup;

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
  private @Nullable Optional<MaritalStatusEnum> maritalStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> birthPlace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> nativePlace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> nationalityCountryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> motherTongue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> religion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> socialCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> socialSubCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> nationalIdType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> nationalId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> visaInfo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 320) String> primaryEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> primaryPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> alternatePhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> remarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> workPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> websiteUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> aboutMe;

  public PersonUpdate honorificId(@Nullable Optional<UUID> honorificId) {
    this.honorificId = honorificId;
    return this;
  }

  /**
   * Name prefix.
   * @return honorificId
   */
  
  @JsonProperty("honorificId")
  public @Nullable Optional<UUID> getHonorificId() {
    return honorificId;
  }

  @JsonProperty("honorificId")
  public void setHonorificId(@Nullable Optional<UUID> honorificId) {
    this.honorificId = honorificId;
  }

  public PersonUpdate firstName(@Nullable Optional<String> firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Given name.
   * @return firstName
   */
  
  @JsonProperty("firstName")
  public @Nullable Optional<String> getFirstName() {
    return firstName;
  }

  @JsonProperty("firstName")
  public void setFirstName(@Nullable Optional<String> firstName) {
    this.firstName = firstName;
  }

  public PersonUpdate middleName(@Nullable Optional<String> middleName) {
    this.middleName = middleName;
    return this;
  }

  /**
   * Middle name.
   * @return middleName
   */
  
  @JsonProperty("middleName")
  public @Nullable Optional<String> getMiddleName() {
    return middleName;
  }

  @JsonProperty("middleName")
  public void setMiddleName(@Nullable Optional<String> middleName) {
    this.middleName = middleName;
  }

  public PersonUpdate lastName(@Nullable Optional<String> lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Family name.
   * @return lastName
   */
  
  @JsonProperty("lastName")
  public @Nullable Optional<String> getLastName() {
    return lastName;
  }

  @JsonProperty("lastName")
  public void setLastName(@Nullable Optional<String> lastName) {
    this.lastName = lastName;
  }

  public PersonUpdate displayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Full name as displayed.
   * @return displayName
   */
  
  @JsonProperty("displayName")
  public @Nullable Optional<String> getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
  }

  public PersonUpdate dateOfBirth(@Nullable Optional<LocalDate> dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  /**
   * Date of birth.
   * @return dateOfBirth
   */
  
  @JsonProperty("dateOfBirth")
  public @Nullable Optional<LocalDate> getDateOfBirth() {
    return dateOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public void setDateOfBirth(@Nullable Optional<LocalDate> dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public PersonUpdate gender(@Nullable Optional<GenderEnum> gender) {
    this.gender = gender;
    return this;
  }

  /**
   * Gender.
   * @return gender
   */
  
  @JsonProperty("gender")
  public @Nullable Optional<GenderEnum> getGender() {
    return gender;
  }

  @JsonProperty("gender")
  public void setGender(@Nullable Optional<GenderEnum> gender) {
    this.gender = gender;
  }

  public PersonUpdate bloodGroup(@Nullable Optional<BloodGroupEnum> bloodGroup) {
    this.bloodGroup = bloodGroup;
    return this;
  }

  /**
   * Blood group.
   * @return bloodGroup
   */
  
  @JsonProperty("bloodGroup")
  public @Nullable Optional<BloodGroupEnum> getBloodGroup() {
    return bloodGroup;
  }

  @JsonProperty("bloodGroup")
  public void setBloodGroup(@Nullable Optional<BloodGroupEnum> bloodGroup) {
    this.bloodGroup = bloodGroup;
  }

  public PersonUpdate maritalStatus(@Nullable Optional<MaritalStatusEnum> maritalStatus) {
    this.maritalStatus = maritalStatus;
    return this;
  }

  /**
   * Marital status.
   * @return maritalStatus
   */
  
  @JsonProperty("maritalStatus")
  public @Nullable Optional<MaritalStatusEnum> getMaritalStatus() {
    return maritalStatus;
  }

  @JsonProperty("maritalStatus")
  public void setMaritalStatus(@Nullable Optional<MaritalStatusEnum> maritalStatus) {
    this.maritalStatus = maritalStatus;
  }

  public PersonUpdate birthPlace(@Nullable Optional<String> birthPlace) {
    this.birthPlace = birthPlace;
    return this;
  }

  /**
   * Place of birth.
   * @return birthPlace
   */
  
  @JsonProperty("birthPlace")
  public @Nullable Optional<String> getBirthPlace() {
    return birthPlace;
  }

  @JsonProperty("birthPlace")
  public void setBirthPlace(@Nullable Optional<String> birthPlace) {
    this.birthPlace = birthPlace;
  }

  public PersonUpdate nativePlace(@Nullable Optional<String> nativePlace) {
    this.nativePlace = nativePlace;
    return this;
  }

  /**
   * Native place / hometown.
   * @return nativePlace
   */
  
  @JsonProperty("nativePlace")
  public @Nullable Optional<String> getNativePlace() {
    return nativePlace;
  }

  @JsonProperty("nativePlace")
  public void setNativePlace(@Nullable Optional<String> nativePlace) {
    this.nativePlace = nativePlace;
  }

  public PersonUpdate nationalityCountryId(@Nullable Optional<UUID> nationalityCountryId) {
    this.nationalityCountryId = nationalityCountryId;
    return this;
  }

  /**
   * Nationality.
   * @return nationalityCountryId
   */
  
  @JsonProperty("nationalityCountryId")
  public @Nullable Optional<UUID> getNationalityCountryId() {
    return nationalityCountryId;
  }

  @JsonProperty("nationalityCountryId")
  public void setNationalityCountryId(@Nullable Optional<UUID> nationalityCountryId) {
    this.nationalityCountryId = nationalityCountryId;
  }

  public PersonUpdate motherTongue(@Nullable Optional<String> motherTongue) {
    this.motherTongue = motherTongue;
    return this;
  }

  /**
   * Mother tongue.
   * @return motherTongue
   */
  
  @JsonProperty("motherTongue")
  public @Nullable Optional<String> getMotherTongue() {
    return motherTongue;
  }

  @JsonProperty("motherTongue")
  public void setMotherTongue(@Nullable Optional<String> motherTongue) {
    this.motherTongue = motherTongue;
  }

  public PersonUpdate religion(@Nullable Optional<String> religion) {
    this.religion = religion;
    return this;
  }

  /**
   * Religion (optional, for statutory reporting).
   * @return religion
   */
  
  @JsonProperty("religion")
  public @Nullable Optional<String> getReligion() {
    return religion;
  }

  @JsonProperty("religion")
  public void setReligion(@Nullable Optional<String> religion) {
    this.religion = religion;
  }

  public PersonUpdate socialCategory(@Nullable Optional<String> socialCategory) {
    this.socialCategory = socialCategory;
    return this;
  }

  /**
   * Government social category used for statutory reporting (e.g. GEN, OBC, SC, ST).
   * @return socialCategory
   */
  
  @JsonProperty("socialCategory")
  public @Nullable Optional<String> getSocialCategory() {
    return socialCategory;
  }

  @JsonProperty("socialCategory")
  public void setSocialCategory(@Nullable Optional<String> socialCategory) {
    this.socialCategory = socialCategory;
  }

  public PersonUpdate socialSubCategory(@Nullable Optional<String> socialSubCategory) {
    this.socialSubCategory = socialSubCategory;
    return this;
  }

  /**
   * Sub-category.
   * @return socialSubCategory
   */
  
  @JsonProperty("socialSubCategory")
  public @Nullable Optional<String> getSocialSubCategory() {
    return socialSubCategory;
  }

  @JsonProperty("socialSubCategory")
  public void setSocialSubCategory(@Nullable Optional<String> socialSubCategory) {
    this.socialSubCategory = socialSubCategory;
  }

  public PersonUpdate nationalIdType(@Nullable Optional<String> nationalIdType) {
    this.nationalIdType = nationalIdType;
    return this;
  }

  /**
   * Type of national id (e.g. AADHAAR, SSN, PASSPORT).
   * @return nationalIdType
   */
  
  @JsonProperty("nationalIdType")
  public @Nullable Optional<String> getNationalIdType() {
    return nationalIdType;
  }

  @JsonProperty("nationalIdType")
  public void setNationalIdType(@Nullable Optional<String> nationalIdType) {
    this.nationalIdType = nationalIdType;
  }

  public PersonUpdate nationalId(@Nullable Optional<String> nationalId) {
    this.nationalId = nationalId;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. National id number, encrypted at rest.
   * @return nationalId
   */
  
  @JsonProperty("nationalId")
  public @Nullable Optional<String> getNationalId() {
    return nationalId;
  }

  @JsonProperty("nationalId")
  public void setNationalId(@Nullable Optional<String> nationalId) {
    this.nationalId = nationalId;
  }

  public PersonUpdate visaInfo(@Nullable Optional<String> visaInfo) {
    this.visaInfo = visaInfo;
    return this;
  }

  /**
   * Visa / residence-permit details.
   * @return visaInfo
   */
  
  @JsonProperty("visaInfo")
  public @Nullable Optional<String> getVisaInfo() {
    return visaInfo;
  }

  @JsonProperty("visaInfo")
  public void setVisaInfo(@Nullable Optional<String> visaInfo) {
    this.visaInfo = visaInfo;
  }

  public PersonUpdate primaryEmail(@Nullable Optional<String> primaryEmail) {
    this.primaryEmail = primaryEmail;
    return this;
  }

  /**
   * Personal e-mail.
   * @return primaryEmail
   */
  
  @JsonProperty("primaryEmail")
  public @Nullable Optional<String> getPrimaryEmail() {
    return primaryEmail;
  }

  @JsonProperty("primaryEmail")
  public void setPrimaryEmail(@Nullable Optional<String> primaryEmail) {
    this.primaryEmail = primaryEmail;
  }

  public PersonUpdate primaryPhone(@Nullable Optional<String> primaryPhone) {
    this.primaryPhone = primaryPhone;
    return this;
  }

  /**
   * Personal phone.
   * @return primaryPhone
   */
  
  @JsonProperty("primaryPhone")
  public @Nullable Optional<String> getPrimaryPhone() {
    return primaryPhone;
  }

  @JsonProperty("primaryPhone")
  public void setPrimaryPhone(@Nullable Optional<String> primaryPhone) {
    this.primaryPhone = primaryPhone;
  }

  public PersonUpdate alternatePhone(@Nullable Optional<String> alternatePhone) {
    this.alternatePhone = alternatePhone;
    return this;
  }

  /**
   * Alternate phone.
   * @return alternatePhone
   */
  
  @JsonProperty("alternatePhone")
  public @Nullable Optional<String> getAlternatePhone() {
    return alternatePhone;
  }

  @JsonProperty("alternatePhone")
  public void setAlternatePhone(@Nullable Optional<String> alternatePhone) {
    this.alternatePhone = alternatePhone;
  }

  public PersonUpdate photoFileId(@Nullable Optional<UUID> photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Profile photo.
   * @return photoFileId
   */
  
  @JsonProperty("photoFileId")
  public @Nullable Optional<UUID> getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable Optional<UUID> photoFileId) {
    this.photoFileId = photoFileId;
  }

  public PersonUpdate remarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Free-text notes about the person.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable Optional<String> getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
  }

  public PersonUpdate workPhone(@Nullable Optional<String> workPhone) {
    this.workPhone = workPhone;
    return this;
  }

  /**
   * Office / work phone.
   * @return workPhone
   */
  
  @JsonProperty("workPhone")
  public @Nullable Optional<String> getWorkPhone() {
    return workPhone;
  }

  @JsonProperty("workPhone")
  public void setWorkPhone(@Nullable Optional<String> workPhone) {
    this.workPhone = workPhone;
  }

  public PersonUpdate websiteUrl(@Nullable Optional<String> websiteUrl) {
    this.websiteUrl = websiteUrl;
    return this;
  }

  /**
   * Personal web page.
   * @return websiteUrl
   */
  
  @JsonProperty("websiteUrl")
  public @Nullable Optional<String> getWebsiteUrl() {
    return websiteUrl;
  }

  @JsonProperty("websiteUrl")
  public void setWebsiteUrl(@Nullable Optional<String> websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public PersonUpdate aboutMe(@Nullable Optional<String> aboutMe) {
    this.aboutMe = aboutMe;
    return this;
  }

  /**
   * Short self-description shown on the profile.
   * @return aboutMe
   */
  
  @JsonProperty("aboutMe")
  public @Nullable Optional<String> getAboutMe() {
    return aboutMe;
  }

  @JsonProperty("aboutMe")
  public void setAboutMe(@Nullable Optional<String> aboutMe) {
    this.aboutMe = aboutMe;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonUpdate personUpdate = (PersonUpdate) o;
    return Objects.equals(this.honorificId, personUpdate.honorificId) &&
        Objects.equals(this.firstName, personUpdate.firstName) &&
        Objects.equals(this.middleName, personUpdate.middleName) &&
        Objects.equals(this.lastName, personUpdate.lastName) &&
        Objects.equals(this.displayName, personUpdate.displayName) &&
        Objects.equals(this.dateOfBirth, personUpdate.dateOfBirth) &&
        Objects.equals(this.gender, personUpdate.gender) &&
        Objects.equals(this.bloodGroup, personUpdate.bloodGroup) &&
        Objects.equals(this.maritalStatus, personUpdate.maritalStatus) &&
        Objects.equals(this.birthPlace, personUpdate.birthPlace) &&
        Objects.equals(this.nativePlace, personUpdate.nativePlace) &&
        Objects.equals(this.nationalityCountryId, personUpdate.nationalityCountryId) &&
        Objects.equals(this.motherTongue, personUpdate.motherTongue) &&
        Objects.equals(this.religion, personUpdate.religion) &&
        Objects.equals(this.socialCategory, personUpdate.socialCategory) &&
        Objects.equals(this.socialSubCategory, personUpdate.socialSubCategory) &&
        Objects.equals(this.nationalIdType, personUpdate.nationalIdType) &&
        Objects.equals(this.nationalId, personUpdate.nationalId) &&
        Objects.equals(this.visaInfo, personUpdate.visaInfo) &&
        Objects.equals(this.primaryEmail, personUpdate.primaryEmail) &&
        Objects.equals(this.primaryPhone, personUpdate.primaryPhone) &&
        Objects.equals(this.alternatePhone, personUpdate.alternatePhone) &&
        Objects.equals(this.photoFileId, personUpdate.photoFileId) &&
        Objects.equals(this.remarks, personUpdate.remarks) &&
        Objects.equals(this.workPhone, personUpdate.workPhone) &&
        Objects.equals(this.websiteUrl, personUpdate.websiteUrl) &&
        Objects.equals(this.aboutMe, personUpdate.aboutMe);
  }

  @Override
  public int hashCode() {
    return Objects.hash(honorificId, firstName, middleName, lastName, displayName, dateOfBirth, gender, bloodGroup, maritalStatus, birthPlace, nativePlace, nationalityCountryId, motherTongue, religion, socialCategory, socialSubCategory, nationalIdType, nationalId, visaInfo, primaryEmail, primaryPhone, alternatePhone, photoFileId, remarks, workPhone, websiteUrl, aboutMe);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PersonUpdate {\n");
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
    sb.append("    visaInfo: ").append(toIndentedString(visaInfo)).append("\n");
    sb.append("    primaryEmail: ").append(toIndentedString(primaryEmail)).append("\n");
    sb.append("    primaryPhone: ").append(toIndentedString(primaryPhone)).append("\n");
    sb.append("    alternatePhone: ").append(toIndentedString(alternatePhone)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
    sb.append("    workPhone: ").append(toIndentedString(workPhone)).append("\n");
    sb.append("    websiteUrl: ").append(toIndentedString(websiteUrl)).append("\n");
    sb.append("    aboutMe: ").append(toIndentedString(aboutMe)).append("\n");
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

