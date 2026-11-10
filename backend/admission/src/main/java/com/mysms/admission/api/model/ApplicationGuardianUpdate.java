// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ApplicationGuardian that can be updated (partial update; see updateMask).
 */

public class ApplicationGuardianUpdate {

  /**
   * Relationship.
   */
  public enum RelationshipEnum {
    FATHER("father"),
    
    MOTHER("mother"),
    
    GUARDIAN("guardian");

    private final String value;

    RelationshipEnum(String value) {
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
    public static RelationshipEnum fromValue(String value) {
      for (RelationshipEnum b : RelationshipEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RelationshipEnum relationship;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID qualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String designation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String occupation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String organisation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String annualIncome;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nationalId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  public ApplicationGuardianUpdate relationship(@Nullable RelationshipEnum relationship) {
    this.relationship = relationship;
    return this;
  }

  /**
   * Relationship.
   * @return relationship
   */
  
  @JsonProperty("relationship")
  public @Nullable RelationshipEnum getRelationship() {
    return relationship;
  }

  @JsonProperty("relationship")
  public void setRelationship(@Nullable RelationshipEnum relationship) {
    this.relationship = relationship;
  }

  public ApplicationGuardianUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ApplicationGuardianUpdate qualificationId(@Nullable UUID qualificationId) {
    this.qualificationId = qualificationId;
    return this;
  }

  /**
   * Qualification.
   * @return qualificationId
   */
  @Valid 
  @JsonProperty("qualificationId")
  public @Nullable UUID getQualificationId() {
    return qualificationId;
  }

  @JsonProperty("qualificationId")
  public void setQualificationId(@Nullable UUID qualificationId) {
    this.qualificationId = qualificationId;
  }

  public ApplicationGuardianUpdate designation(@Nullable String designation) {
    this.designation = designation;
    return this;
  }

  /**
   * Designation.
   * @return designation
   */
  @Size(max = 120) 
  @JsonProperty("designation")
  public @Nullable String getDesignation() {
    return designation;
  }

  @JsonProperty("designation")
  public void setDesignation(@Nullable String designation) {
    this.designation = designation;
  }

  public ApplicationGuardianUpdate occupation(@Nullable String occupation) {
    this.occupation = occupation;
    return this;
  }

  /**
   * Occupation.
   * @return occupation
   */
  @Size(max = 120) 
  @JsonProperty("occupation")
  public @Nullable String getOccupation() {
    return occupation;
  }

  @JsonProperty("occupation")
  public void setOccupation(@Nullable String occupation) {
    this.occupation = occupation;
  }

  public ApplicationGuardianUpdate organisation(@Nullable String organisation) {
    this.organisation = organisation;
    return this;
  }

  /**
   * Employer.
   * @return organisation
   */
  @Size(max = 200) 
  @JsonProperty("organisation")
  public @Nullable String getOrganisation() {
    return organisation;
  }

  @JsonProperty("organisation")
  public void setOrganisation(@Nullable String organisation) {
    this.organisation = organisation;
  }

  public ApplicationGuardianUpdate annualIncome(@Nullable String annualIncome) {
    this.annualIncome = annualIncome;
    return this;
  }

  /**
   * Annual income.
   * @return annualIncome
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("annualIncome")
  public @Nullable String getAnnualIncome() {
    return annualIncome;
  }

  @JsonProperty("annualIncome")
  public void setAnnualIncome(@Nullable String annualIncome) {
    this.annualIncome = annualIncome;
  }

  public ApplicationGuardianUpdate mobile(@Nullable String mobile) {
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

  public ApplicationGuardianUpdate email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail.
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

  public ApplicationGuardianUpdate nationalId(@Nullable String nationalId) {
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

  public ApplicationGuardianUpdate photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Photo.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationGuardianUpdate applicationGuardianUpdate = (ApplicationGuardianUpdate) o;
    return Objects.equals(this.relationship, applicationGuardianUpdate.relationship) &&
        Objects.equals(this.name, applicationGuardianUpdate.name) &&
        Objects.equals(this.qualificationId, applicationGuardianUpdate.qualificationId) &&
        Objects.equals(this.designation, applicationGuardianUpdate.designation) &&
        Objects.equals(this.occupation, applicationGuardianUpdate.occupation) &&
        Objects.equals(this.organisation, applicationGuardianUpdate.organisation) &&
        Objects.equals(this.annualIncome, applicationGuardianUpdate.annualIncome) &&
        Objects.equals(this.mobile, applicationGuardianUpdate.mobile) &&
        Objects.equals(this.email, applicationGuardianUpdate.email) &&
        Objects.equals(this.nationalId, applicationGuardianUpdate.nationalId) &&
        Objects.equals(this.photoFileId, applicationGuardianUpdate.photoFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relationship, name, qualificationId, designation, occupation, organisation, annualIncome, mobile, email, nationalId, photoFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationGuardianUpdate {\n");
    sb.append("    relationship: ").append(toIndentedString(relationship)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    qualificationId: ").append(toIndentedString(qualificationId)).append("\n");
    sb.append("    designation: ").append(toIndentedString(designation)).append("\n");
    sb.append("    occupation: ").append(toIndentedString(occupation)).append("\n");
    sb.append("    organisation: ").append(toIndentedString(organisation)).append("\n");
    sb.append("    annualIncome: ").append(toIndentedString(annualIncome)).append("\n");
    sb.append("    mobile: ").append(toIndentedString(mobile)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    nationalId: ").append(toIndentedString(nationalId)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
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

