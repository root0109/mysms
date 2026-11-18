// Owned API contract of sis.
package com.mysms.sis.api.model;

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
 * Fields of Guardian that can be updated (partial update; see updateMask).
 */

public class GuardianUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  /**
   * Profession.
   */
  public enum ProfessionEnum {
    ADMIN("admin"),
    
    BUSINESS("business"),
    
    CENTRAL_GOVERNMENT("central_government"),
    
    STATE_GOVERNMENT("state_government"),
    
    PRIVATE_SECTOR("private_sector"),
    
    HOME_MAKER("home_maker"),
    
    TEACHER("teacher"),
    
    LIBRARIAN("librarian"),
    
    OTHER("other");

    private final String value;

    ProfessionEnum(String value) {
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
    public static ProfessionEnum fromValue(String value) {
      for (ProfessionEnum b : ProfessionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ProfessionEnum profession;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subOccupation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String designation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String organizationName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String annualIncome;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID qualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  public GuardianUpdate personId(@Nullable UUID personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  @Valid 
  @JsonProperty("personId")
  public @Nullable UUID getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable UUID personId) {
    this.personId = personId;
  }

  public GuardianUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Guardian login account.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public GuardianUpdate profession(@Nullable ProfessionEnum profession) {
    this.profession = profession;
    return this;
  }

  /**
   * Profession.
   * @return profession
   */
  
  @JsonProperty("profession")
  public @Nullable ProfessionEnum getProfession() {
    return profession;
  }

  @JsonProperty("profession")
  public void setProfession(@Nullable ProfessionEnum profession) {
    this.profession = profession;
  }

  public GuardianUpdate subOccupation(@Nullable String subOccupation) {
    this.subOccupation = subOccupation;
    return this;
  }

  /**
   * Occupation detail.
   * @return subOccupation
   */
  @Size(max = 120) 
  @JsonProperty("subOccupation")
  public @Nullable String getSubOccupation() {
    return subOccupation;
  }

  @JsonProperty("subOccupation")
  public void setSubOccupation(@Nullable String subOccupation) {
    this.subOccupation = subOccupation;
  }

  public GuardianUpdate designation(@Nullable String designation) {
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

  public GuardianUpdate organizationName(@Nullable String organizationName) {
    this.organizationName = organizationName;
    return this;
  }

  /**
   * Employer.
   * @return organizationName
   */
  @Size(max = 200) 
  @JsonProperty("organizationName")
  public @Nullable String getOrganizationName() {
    return organizationName;
  }

  @JsonProperty("organizationName")
  public void setOrganizationName(@Nullable String organizationName) {
    this.organizationName = organizationName;
  }

  public GuardianUpdate annualIncome(@Nullable String annualIncome) {
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

  public GuardianUpdate qualificationId(@Nullable UUID qualificationId) {
    this.qualificationId = qualificationId;
    return this;
  }

  /**
   * Highest qualification.
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

  public GuardianUpdate mobile(@Nullable String mobile) {
    this.mobile = mobile;
    return this;
  }

  /**
   * Contact mobile used for school communication.
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

  public GuardianUpdate email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Contact e-mail used for school communication.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuardianUpdate guardianUpdate = (GuardianUpdate) o;
    return Objects.equals(this.personId, guardianUpdate.personId) &&
        Objects.equals(this.userId, guardianUpdate.userId) &&
        Objects.equals(this.profession, guardianUpdate.profession) &&
        Objects.equals(this.subOccupation, guardianUpdate.subOccupation) &&
        Objects.equals(this.designation, guardianUpdate.designation) &&
        Objects.equals(this.organizationName, guardianUpdate.organizationName) &&
        Objects.equals(this.annualIncome, guardianUpdate.annualIncome) &&
        Objects.equals(this.qualificationId, guardianUpdate.qualificationId) &&
        Objects.equals(this.mobile, guardianUpdate.mobile) &&
        Objects.equals(this.email, guardianUpdate.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, userId, profession, subOccupation, designation, organizationName, annualIncome, qualificationId, mobile, email);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GuardianUpdate {\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    profession: ").append(toIndentedString(profession)).append("\n");
    sb.append("    subOccupation: ").append(toIndentedString(subOccupation)).append("\n");
    sb.append("    designation: ").append(toIndentedString(designation)).append("\n");
    sb.append("    organizationName: ").append(toIndentedString(organizationName)).append("\n");
    sb.append("    annualIncome: ").append(toIndentedString(annualIncome)).append("\n");
    sb.append("    qualificationId: ").append(toIndentedString(qualificationId)).append("\n");
    sb.append("    mobile: ").append(toIndentedString(mobile)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
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

