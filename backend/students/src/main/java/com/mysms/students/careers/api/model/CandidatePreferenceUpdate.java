// Owned API contract of careers.
package com.mysms.students.careers.api.model;

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
 * Fields of CandidatePreference that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CandidatePreferenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> personId;

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
  private @Nullable Optional<OpenToEnum> openTo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> preferredLocation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> dreamCompanies;

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
  private @Nullable Optional<AttireEnum> attire;

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
  private @Nullable Optional<CollaborationEnum> collaboration;

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
  private @Nullable Optional<RoleClarityEnum> roleClarity;

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
  private @Nullable Optional<LocationPreferenceEnum> locationPreference;

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
  private @Nullable Optional<WorkHoursEnum> workHours;

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
  private @Nullable Optional<CompanyTypeEnum> companyType;

  public CandidatePreferenceUpdate personId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  
  @JsonProperty("personId")
  public @Nullable Optional<UUID> getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
  }

  public CandidatePreferenceUpdate openTo(@Nullable Optional<OpenToEnum> openTo) {
    this.openTo = openTo;
    return this;
  }

  /**
   * Open to work or hiring.
   * @return openTo
   */
  
  @JsonProperty("openTo")
  public @Nullable Optional<OpenToEnum> getOpenTo() {
    return openTo;
  }

  @JsonProperty("openTo")
  public void setOpenTo(@Nullable Optional<OpenToEnum> openTo) {
    this.openTo = openTo;
  }

  public CandidatePreferenceUpdate preferredLocation(@Nullable Optional<String> preferredLocation) {
    this.preferredLocation = preferredLocation;
    return this;
  }

  /**
   * Preferred location.
   * @return preferredLocation
   */
  
  @JsonProperty("preferredLocation")
  public @Nullable Optional<String> getPreferredLocation() {
    return preferredLocation;
  }

  @JsonProperty("preferredLocation")
  public void setPreferredLocation(@Nullable Optional<String> preferredLocation) {
    this.preferredLocation = preferredLocation;
  }

  public CandidatePreferenceUpdate dreamCompanies(@Nullable Optional<String> dreamCompanies) {
    this.dreamCompanies = dreamCompanies;
    return this;
  }

  /**
   * Dream companies.
   * @return dreamCompanies
   */
  
  @JsonProperty("dreamCompanies")
  public @Nullable Optional<String> getDreamCompanies() {
    return dreamCompanies;
  }

  @JsonProperty("dreamCompanies")
  public void setDreamCompanies(@Nullable Optional<String> dreamCompanies) {
    this.dreamCompanies = dreamCompanies;
  }

  public CandidatePreferenceUpdate attire(@Nullable Optional<AttireEnum> attire) {
    this.attire = attire;
    return this;
  }

  /**
   * Attire preference.
   * @return attire
   */
  
  @JsonProperty("attire")
  public @Nullable Optional<AttireEnum> getAttire() {
    return attire;
  }

  @JsonProperty("attire")
  public void setAttire(@Nullable Optional<AttireEnum> attire) {
    this.attire = attire;
  }

  public CandidatePreferenceUpdate collaboration(@Nullable Optional<CollaborationEnum> collaboration) {
    this.collaboration = collaboration;
    return this;
  }

  /**
   * Collaboration preference.
   * @return collaboration
   */
  
  @JsonProperty("collaboration")
  public @Nullable Optional<CollaborationEnum> getCollaboration() {
    return collaboration;
  }

  @JsonProperty("collaboration")
  public void setCollaboration(@Nullable Optional<CollaborationEnum> collaboration) {
    this.collaboration = collaboration;
  }

  public CandidatePreferenceUpdate roleClarity(@Nullable Optional<RoleClarityEnum> roleClarity) {
    this.roleClarity = roleClarity;
    return this;
  }

  /**
   * Role-structure preference.
   * @return roleClarity
   */
  
  @JsonProperty("roleClarity")
  public @Nullable Optional<RoleClarityEnum> getRoleClarity() {
    return roleClarity;
  }

  @JsonProperty("roleClarity")
  public void setRoleClarity(@Nullable Optional<RoleClarityEnum> roleClarity) {
    this.roleClarity = roleClarity;
  }

  public CandidatePreferenceUpdate locationPreference(@Nullable Optional<LocationPreferenceEnum> locationPreference) {
    this.locationPreference = locationPreference;
    return this;
  }

  /**
   * Travel / commute preference.
   * @return locationPreference
   */
  
  @JsonProperty("locationPreference")
  public @Nullable Optional<LocationPreferenceEnum> getLocationPreference() {
    return locationPreference;
  }

  @JsonProperty("locationPreference")
  public void setLocationPreference(@Nullable Optional<LocationPreferenceEnum> locationPreference) {
    this.locationPreference = locationPreference;
  }

  public CandidatePreferenceUpdate workHours(@Nullable Optional<WorkHoursEnum> workHours) {
    this.workHours = workHours;
    return this;
  }

  /**
   * Working-hours preference.
   * @return workHours
   */
  
  @JsonProperty("workHours")
  public @Nullable Optional<WorkHoursEnum> getWorkHours() {
    return workHours;
  }

  @JsonProperty("workHours")
  public void setWorkHours(@Nullable Optional<WorkHoursEnum> workHours) {
    this.workHours = workHours;
  }

  public CandidatePreferenceUpdate companyType(@Nullable Optional<CompanyTypeEnum> companyType) {
    this.companyType = companyType;
    return this;
  }

  /**
   * Company-type preference.
   * @return companyType
   */
  
  @JsonProperty("companyType")
  public @Nullable Optional<CompanyTypeEnum> getCompanyType() {
    return companyType;
  }

  @JsonProperty("companyType")
  public void setCompanyType(@Nullable Optional<CompanyTypeEnum> companyType) {
    this.companyType = companyType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CandidatePreferenceUpdate candidatePreferenceUpdate = (CandidatePreferenceUpdate) o;
    return Objects.equals(this.personId, candidatePreferenceUpdate.personId) &&
        Objects.equals(this.openTo, candidatePreferenceUpdate.openTo) &&
        Objects.equals(this.preferredLocation, candidatePreferenceUpdate.preferredLocation) &&
        Objects.equals(this.dreamCompanies, candidatePreferenceUpdate.dreamCompanies) &&
        Objects.equals(this.attire, candidatePreferenceUpdate.attire) &&
        Objects.equals(this.collaboration, candidatePreferenceUpdate.collaboration) &&
        Objects.equals(this.roleClarity, candidatePreferenceUpdate.roleClarity) &&
        Objects.equals(this.locationPreference, candidatePreferenceUpdate.locationPreference) &&
        Objects.equals(this.workHours, candidatePreferenceUpdate.workHours) &&
        Objects.equals(this.companyType, candidatePreferenceUpdate.companyType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, openTo, preferredLocation, dreamCompanies, attire, collaboration, roleClarity, locationPreference, workHours, companyType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CandidatePreferenceUpdate {\n");
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

