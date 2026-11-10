// Owned API contract of careers (JAVA-CODING-RULES §7); first materialized from api/openapi/careers.yaml on 2026-11-10.
package com.mysms.careers.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A person&#39;s job-search preferences.
 */

public class CandidatePreference {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID personId;

  /**
   * Open to work or hiring.
   */
  public enum OpenToEnum {
    WORK("work"),
    
    HIRING("hiring");

    private final String value;

    OpenToEnum(String value) {
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
    public static OpenToEnum fromValue(String value) {
      for (OpenToEnum b : OpenToEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OpenToEnum openTo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String preferredLocation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String dreamCompanies;

  /**
   * Attire preference.
   */
  public enum AttireEnum {
    CASUAL("casual"),
    
    FORMAL("formal");

    private final String value;

    AttireEnum(String value) {
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
    public static AttireEnum fromValue(String value) {
      for (AttireEnum b : AttireEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AttireEnum attire;

  /**
   * Collaboration preference.
   */
  public enum CollaborationEnum {
    INDIVIDUAL("individual"),
    
    TEAM("team"),
    
    BOTH("both");

    private final String value;

    CollaborationEnum(String value) {
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
    public static CollaborationEnum fromValue(String value) {
      for (CollaborationEnum b : CollaborationEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable CollaborationEnum collaboration;

  /**
   * Role-structure preference.
   */
  public enum RoleClarityEnum {
    CLEARLY_DEFINED("clearly_defined"),
    
    UNSTRUCTURED("unstructured");

    private final String value;

    RoleClarityEnum(String value) {
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
    public static RoleClarityEnum fromValue(String value) {
      for (RoleClarityEnum b : RoleClarityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RoleClarityEnum roleClarity;

  /**
   * Travel / commute preference.
   */
  public enum LocationPreferenceEnum {
    TRAVEL("travel"),
    
    NEAR_HOME("near_home");

    private final String value;

    LocationPreferenceEnum(String value) {
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
    public static LocationPreferenceEnum fromValue(String value) {
      for (LocationPreferenceEnum b : LocationPreferenceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable LocationPreferenceEnum locationPreference;

  /**
   * Working-hours preference.
   */
  public enum WorkHoursEnum {
    FLEXIBLE("flexible"),
    
    FIXED("fixed");

    private final String value;

    WorkHoursEnum(String value) {
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
    public static WorkHoursEnum fromValue(String value) {
      for (WorkHoursEnum b : WorkHoursEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable WorkHoursEnum workHours;

  /**
   * Company-type preference.
   */
  public enum CompanyTypeEnum {
    CORPORATE("corporate"),
    
    STARTUP("startup");

    private final String value;

    CompanyTypeEnum(String value) {
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
    public static CompanyTypeEnum fromValue(String value) {
      for (CompanyTypeEnum b : CompanyTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable CompanyTypeEnum companyType;

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

  public CandidatePreference() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CandidatePreference(UUID personId) {
    this.personId = personId;
  }

  public CandidatePreference id(@Nullable UUID id) {
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

  public CandidatePreference personId(UUID personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  @NotNull @Valid 
  @JsonProperty("personId")
  public UUID getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(UUID personId) {
    this.personId = personId;
  }

  public CandidatePreference openTo(@Nullable OpenToEnum openTo) {
    this.openTo = openTo;
    return this;
  }

  /**
   * Open to work or hiring.
   * @return openTo
   */
  
  @JsonProperty("openTo")
  public @Nullable OpenToEnum getOpenTo() {
    return openTo;
  }

  @JsonProperty("openTo")
  public void setOpenTo(@Nullable OpenToEnum openTo) {
    this.openTo = openTo;
  }

  public CandidatePreference preferredLocation(@Nullable String preferredLocation) {
    this.preferredLocation = preferredLocation;
    return this;
  }

  /**
   * Preferred location.
   * @return preferredLocation
   */
  @Size(max = 150) 
  @JsonProperty("preferredLocation")
  public @Nullable String getPreferredLocation() {
    return preferredLocation;
  }

  @JsonProperty("preferredLocation")
  public void setPreferredLocation(@Nullable String preferredLocation) {
    this.preferredLocation = preferredLocation;
  }

  public CandidatePreference dreamCompanies(@Nullable String dreamCompanies) {
    this.dreamCompanies = dreamCompanies;
    return this;
  }

  /**
   * Dream companies.
   * @return dreamCompanies
   */
  
  @JsonProperty("dreamCompanies")
  public @Nullable String getDreamCompanies() {
    return dreamCompanies;
  }

  @JsonProperty("dreamCompanies")
  public void setDreamCompanies(@Nullable String dreamCompanies) {
    this.dreamCompanies = dreamCompanies;
  }

  public CandidatePreference attire(@Nullable AttireEnum attire) {
    this.attire = attire;
    return this;
  }

  /**
   * Attire preference.
   * @return attire
   */
  
  @JsonProperty("attire")
  public @Nullable AttireEnum getAttire() {
    return attire;
  }

  @JsonProperty("attire")
  public void setAttire(@Nullable AttireEnum attire) {
    this.attire = attire;
  }

  public CandidatePreference collaboration(@Nullable CollaborationEnum collaboration) {
    this.collaboration = collaboration;
    return this;
  }

  /**
   * Collaboration preference.
   * @return collaboration
   */
  
  @JsonProperty("collaboration")
  public @Nullable CollaborationEnum getCollaboration() {
    return collaboration;
  }

  @JsonProperty("collaboration")
  public void setCollaboration(@Nullable CollaborationEnum collaboration) {
    this.collaboration = collaboration;
  }

  public CandidatePreference roleClarity(@Nullable RoleClarityEnum roleClarity) {
    this.roleClarity = roleClarity;
    return this;
  }

  /**
   * Role-structure preference.
   * @return roleClarity
   */
  
  @JsonProperty("roleClarity")
  public @Nullable RoleClarityEnum getRoleClarity() {
    return roleClarity;
  }

  @JsonProperty("roleClarity")
  public void setRoleClarity(@Nullable RoleClarityEnum roleClarity) {
    this.roleClarity = roleClarity;
  }

  public CandidatePreference locationPreference(@Nullable LocationPreferenceEnum locationPreference) {
    this.locationPreference = locationPreference;
    return this;
  }

  /**
   * Travel / commute preference.
   * @return locationPreference
   */
  
  @JsonProperty("locationPreference")
  public @Nullable LocationPreferenceEnum getLocationPreference() {
    return locationPreference;
  }

  @JsonProperty("locationPreference")
  public void setLocationPreference(@Nullable LocationPreferenceEnum locationPreference) {
    this.locationPreference = locationPreference;
  }

  public CandidatePreference workHours(@Nullable WorkHoursEnum workHours) {
    this.workHours = workHours;
    return this;
  }

  /**
   * Working-hours preference.
   * @return workHours
   */
  
  @JsonProperty("workHours")
  public @Nullable WorkHoursEnum getWorkHours() {
    return workHours;
  }

  @JsonProperty("workHours")
  public void setWorkHours(@Nullable WorkHoursEnum workHours) {
    this.workHours = workHours;
  }

  public CandidatePreference companyType(@Nullable CompanyTypeEnum companyType) {
    this.companyType = companyType;
    return this;
  }

  /**
   * Company-type preference.
   * @return companyType
   */
  
  @JsonProperty("companyType")
  public @Nullable CompanyTypeEnum getCompanyType() {
    return companyType;
  }

  @JsonProperty("companyType")
  public void setCompanyType(@Nullable CompanyTypeEnum companyType) {
    this.companyType = companyType;
  }

  public CandidatePreference createTime(@Nullable OffsetDateTime createTime) {
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

  public CandidatePreference createdBy(@Nullable UUID createdBy) {
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

  public CandidatePreference updateTime(@Nullable OffsetDateTime updateTime) {
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

  public CandidatePreference updatedBy(@Nullable UUID updatedBy) {
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

  public CandidatePreference etag(@Nullable String etag) {
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
    CandidatePreference candidatePreference = (CandidatePreference) o;
    return Objects.equals(this.id, candidatePreference.id) &&
        Objects.equals(this.personId, candidatePreference.personId) &&
        Objects.equals(this.openTo, candidatePreference.openTo) &&
        Objects.equals(this.preferredLocation, candidatePreference.preferredLocation) &&
        Objects.equals(this.dreamCompanies, candidatePreference.dreamCompanies) &&
        Objects.equals(this.attire, candidatePreference.attire) &&
        Objects.equals(this.collaboration, candidatePreference.collaboration) &&
        Objects.equals(this.roleClarity, candidatePreference.roleClarity) &&
        Objects.equals(this.locationPreference, candidatePreference.locationPreference) &&
        Objects.equals(this.workHours, candidatePreference.workHours) &&
        Objects.equals(this.companyType, candidatePreference.companyType) &&
        Objects.equals(this.createTime, candidatePreference.createTime) &&
        Objects.equals(this.createdBy, candidatePreference.createdBy) &&
        Objects.equals(this.updateTime, candidatePreference.updateTime) &&
        Objects.equals(this.updatedBy, candidatePreference.updatedBy) &&
        Objects.equals(this.etag, candidatePreference.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, personId, openTo, preferredLocation, dreamCompanies, attire, collaboration, roleClarity, locationPreference, workHours, companyType, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CandidatePreference {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    openTo: ").append(toIndentedString(openTo)).append("\n");
    sb.append("    preferredLocation: ").append(toIndentedString(preferredLocation)).append("\n");
    sb.append("    dreamCompanies: ").append(toIndentedString(dreamCompanies)).append("\n");
    sb.append("    attire: ").append(toIndentedString(attire)).append("\n");
    sb.append("    collaboration: ").append(toIndentedString(collaboration)).append("\n");
    sb.append("    roleClarity: ").append(toIndentedString(roleClarity)).append("\n");
    sb.append("    locationPreference: ").append(toIndentedString(locationPreference)).append("\n");
    sb.append("    workHours: ").append(toIndentedString(workHours)).append("\n");
    sb.append("    companyType: ").append(toIndentedString(companyType)).append("\n");
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

