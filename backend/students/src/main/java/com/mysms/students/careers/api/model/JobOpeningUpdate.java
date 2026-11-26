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
 * Fields of JobOpening that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class JobOpeningUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> postedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> countryId;

  /**
   * Employment type.
   */
  public enum EmploymentTypeEnum {
    FULL_TIME("full_time"),
    
    PART_TIME("part_time"),
    
    FREELANCE("freelance"),
    
    CONTRACT("contract");

    private final String value;

    EmploymentTypeEnum(String value) {
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
    public static EmploymentTypeEnum fromValue(String value) {
      for (EmploymentTypeEnum b : EmploymentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull EmploymentTypeEnum> employmentType;

  /**
   * Work mode.
   */
  public enum WorkModeEnum {
    REMOTE("remote"),
    
    HYBRID("hybrid"),
    
    ON_SITE("on_site");

    private final String value;

    WorkModeEnum(String value) {
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
    public static WorkModeEnum fromValue(String value) {
      for (WorkModeEnum b : WorkModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<WorkModeEnum> workMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> companyName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 500) String> companyWebsite;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 320) String> companyEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> companyLogoFileId;

  public JobOpeningUpdate postedByUserId(@Nullable Optional<UUID> postedByUserId) {
    this.postedByUserId = postedByUserId;
    return this;
  }

  /**
   * Poster.
   * @return postedByUserId
   */
  
  @JsonProperty("postedByUserId")
  public @Nullable Optional<UUID> getPostedByUserId() {
    return postedByUserId;
  }

  @JsonProperty("postedByUserId")
  public void setPostedByUserId(@Nullable Optional<UUID> postedByUserId) {
    this.postedByUserId = postedByUserId;
  }

  public JobOpeningUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Job title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public JobOpeningUpdate location(@Nullable Optional<String> location) {
    this.location = location;
    return this;
  }

  /**
   * Location.
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable Optional<String> getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable Optional<String> location) {
    this.location = location;
  }

  public JobOpeningUpdate countryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  
  @JsonProperty("countryId")
  public @Nullable Optional<UUID> getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
  }

  public JobOpeningUpdate employmentType(@Nullable Optional<EmploymentTypeEnum> employmentType) {
    this.employmentType = employmentType;
    return this;
  }

  /**
   * Employment type.
   * @return employmentType
   */
  
  @JsonProperty("employmentType")
  public @Nullable Optional<EmploymentTypeEnum> getEmploymentType() {
    return employmentType;
  }

  @JsonProperty("employmentType")
  public void setEmploymentType(@Nullable Optional<EmploymentTypeEnum> employmentType) {
    this.employmentType = employmentType;
  }

  public JobOpeningUpdate workMode(@Nullable Optional<WorkModeEnum> workMode) {
    this.workMode = workMode;
    return this;
  }

  /**
   * Work mode.
   * @return workMode
   */
  
  @JsonProperty("workMode")
  public @Nullable Optional<WorkModeEnum> getWorkMode() {
    return workMode;
  }

  @JsonProperty("workMode")
  public void setWorkMode(@Nullable Optional<WorkModeEnum> workMode) {
    this.workMode = workMode;
  }

  public JobOpeningUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public JobOpeningUpdate companyName(@Nullable Optional<String> companyName) {
    this.companyName = companyName;
    return this;
  }

  /**
   * Company.
   * @return companyName
   */
  
  @JsonProperty("companyName")
  public @Nullable Optional<String> getCompanyName() {
    return companyName;
  }

  @JsonProperty("companyName")
  public void setCompanyName(@Nullable Optional<String> companyName) {
    this.companyName = companyName;
  }

  public JobOpeningUpdate companyWebsite(@Nullable Optional<String> companyWebsite) {
    this.companyWebsite = companyWebsite;
    return this;
  }

  /**
   * Company website.
   * @return companyWebsite
   */
  
  @JsonProperty("companyWebsite")
  public @Nullable Optional<String> getCompanyWebsite() {
    return companyWebsite;
  }

  @JsonProperty("companyWebsite")
  public void setCompanyWebsite(@Nullable Optional<String> companyWebsite) {
    this.companyWebsite = companyWebsite;
  }

  public JobOpeningUpdate companyEmail(@Nullable Optional<String> companyEmail) {
    this.companyEmail = companyEmail;
    return this;
  }

  /**
   * Company e-mail.
   * @return companyEmail
   */
  
  @JsonProperty("companyEmail")
  public @Nullable Optional<String> getCompanyEmail() {
    return companyEmail;
  }

  @JsonProperty("companyEmail")
  public void setCompanyEmail(@Nullable Optional<String> companyEmail) {
    this.companyEmail = companyEmail;
  }

  public JobOpeningUpdate companyLogoFileId(@Nullable Optional<UUID> companyLogoFileId) {
    this.companyLogoFileId = companyLogoFileId;
    return this;
  }

  /**
   * Company logo.
   * @return companyLogoFileId
   */
  
  @JsonProperty("companyLogoFileId")
  public @Nullable Optional<UUID> getCompanyLogoFileId() {
    return companyLogoFileId;
  }

  @JsonProperty("companyLogoFileId")
  public void setCompanyLogoFileId(@Nullable Optional<UUID> companyLogoFileId) {
    this.companyLogoFileId = companyLogoFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobOpeningUpdate jobOpeningUpdate = (JobOpeningUpdate) o;
    return Objects.equals(this.postedByUserId, jobOpeningUpdate.postedByUserId) &&
        Objects.equals(this.title, jobOpeningUpdate.title) &&
        Objects.equals(this.location, jobOpeningUpdate.location) &&
        Objects.equals(this.countryId, jobOpeningUpdate.countryId) &&
        Objects.equals(this.employmentType, jobOpeningUpdate.employmentType) &&
        Objects.equals(this.workMode, jobOpeningUpdate.workMode) &&
        Objects.equals(this.description, jobOpeningUpdate.description) &&
        Objects.equals(this.companyName, jobOpeningUpdate.companyName) &&
        Objects.equals(this.companyWebsite, jobOpeningUpdate.companyWebsite) &&
        Objects.equals(this.companyEmail, jobOpeningUpdate.companyEmail) &&
        Objects.equals(this.companyLogoFileId, jobOpeningUpdate.companyLogoFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postedByUserId, title, location, countryId, employmentType, workMode, description, companyName, companyWebsite, companyEmail, companyLogoFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobOpeningUpdate {\n");
    sb.append("    postedByUserId: ").append(toIndentedString(postedByUserId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    employmentType: ").append(toIndentedString(employmentType)).append("\n");
    sb.append("    workMode: ").append(toIndentedString(workMode)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
    sb.append("    companyWebsite: ").append(toIndentedString(companyWebsite)).append("\n");
    sb.append("    companyEmail: ").append(toIndentedString(companyEmail)).append("\n");
    sb.append("    companyLogoFileId: ").append(toIndentedString(companyLogoFileId)).append("\n");
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

