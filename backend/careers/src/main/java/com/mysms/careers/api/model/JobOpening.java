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
 * Job posted on the job board.
 */

public class JobOpening {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID postedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID countryId;

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
  private EmploymentTypeEnum employmentType;

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
  private @Nullable WorkModeEnum workMode;

  /**
   * State. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    OPEN("open"),
    
    CLOSED("closed");

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
  private String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String companyName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String companyWebsite;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String companyEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID companyLogoFileId;

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

  public JobOpening() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobOpening(String title, String location, UUID countryId, EmploymentTypeEnum employmentType, String description, String companyName, String companyWebsite, String companyEmail, UUID companyLogoFileId) {
    this.title = title;
    this.location = location;
    this.countryId = countryId;
    this.employmentType = employmentType;
    this.description = description;
    this.companyName = companyName;
    this.companyWebsite = companyWebsite;
    this.companyEmail = companyEmail;
    this.companyLogoFileId = companyLogoFileId;
  }

  public JobOpening id(@Nullable UUID id) {
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

  public JobOpening postedByUserId(@Nullable UUID postedByUserId) {
    this.postedByUserId = postedByUserId;
    return this;
  }

  /**
   * Poster.
   * @return postedByUserId
   */
  @Valid 
  @JsonProperty("postedByUserId")
  public @Nullable UUID getPostedByUserId() {
    return postedByUserId;
  }

  @JsonProperty("postedByUserId")
  public void setPostedByUserId(@Nullable UUID postedByUserId) {
    this.postedByUserId = postedByUserId;
  }

  public JobOpening title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Job title.
   * @return title
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public JobOpening location(String location) {
    this.location = location;
    return this;
  }

  /**
   * Location.
   * @return location
   */
  @NotNull @Size(max = 150) 
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(String location) {
    this.location = location;
  }

  public JobOpening countryId(UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  @NotNull @Valid 
  @JsonProperty("countryId")
  public UUID getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(UUID countryId) {
    this.countryId = countryId;
  }

  public JobOpening employmentType(EmploymentTypeEnum employmentType) {
    this.employmentType = employmentType;
    return this;
  }

  /**
   * Employment type.
   * @return employmentType
   */
  @NotNull 
  @JsonProperty("employmentType")
  public EmploymentTypeEnum getEmploymentType() {
    return employmentType;
  }

  @JsonProperty("employmentType")
  public void setEmploymentType(EmploymentTypeEnum employmentType) {
    this.employmentType = employmentType;
  }

  public JobOpening workMode(@Nullable WorkModeEnum workMode) {
    this.workMode = workMode;
    return this;
  }

  /**
   * Work mode.
   * @return workMode
   */
  
  @JsonProperty("workMode")
  public @Nullable WorkModeEnum getWorkMode() {
    return workMode;
  }

  @JsonProperty("workMode")
  public void setWorkMode(@Nullable WorkModeEnum workMode) {
    this.workMode = workMode;
  }

  public JobOpening status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State. Changed only through the workflow operations.
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

  public JobOpening description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  @NotNull 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  public JobOpening companyName(String companyName) {
    this.companyName = companyName;
    return this;
  }

  /**
   * Company.
   * @return companyName
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("companyName")
  public String getCompanyName() {
    return companyName;
  }

  @JsonProperty("companyName")
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public JobOpening companyWebsite(String companyWebsite) {
    this.companyWebsite = companyWebsite;
    return this;
  }

  /**
   * Company website.
   * @return companyWebsite
   */
  @NotNull @Size(max = 500) 
  @JsonProperty("companyWebsite")
  public String getCompanyWebsite() {
    return companyWebsite;
  }

  @JsonProperty("companyWebsite")
  public void setCompanyWebsite(String companyWebsite) {
    this.companyWebsite = companyWebsite;
  }

  public JobOpening companyEmail(String companyEmail) {
    this.companyEmail = companyEmail;
    return this;
  }

  /**
   * Company e-mail.
   * @return companyEmail
   */
  @NotNull @Size(max = 320) 
  @JsonProperty("companyEmail")
  public String getCompanyEmail() {
    return companyEmail;
  }

  @JsonProperty("companyEmail")
  public void setCompanyEmail(String companyEmail) {
    this.companyEmail = companyEmail;
  }

  public JobOpening companyLogoFileId(UUID companyLogoFileId) {
    this.companyLogoFileId = companyLogoFileId;
    return this;
  }

  /**
   * Company logo.
   * @return companyLogoFileId
   */
  @NotNull @Valid 
  @JsonProperty("companyLogoFileId")
  public UUID getCompanyLogoFileId() {
    return companyLogoFileId;
  }

  @JsonProperty("companyLogoFileId")
  public void setCompanyLogoFileId(UUID companyLogoFileId) {
    this.companyLogoFileId = companyLogoFileId;
  }

  public JobOpening createTime(@Nullable OffsetDateTime createTime) {
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

  public JobOpening createdBy(@Nullable UUID createdBy) {
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

  public JobOpening updateTime(@Nullable OffsetDateTime updateTime) {
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

  public JobOpening updatedBy(@Nullable UUID updatedBy) {
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

  public JobOpening etag(@Nullable String etag) {
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
    JobOpening jobOpening = (JobOpening) o;
    return Objects.equals(this.id, jobOpening.id) &&
        Objects.equals(this.postedByUserId, jobOpening.postedByUserId) &&
        Objects.equals(this.title, jobOpening.title) &&
        Objects.equals(this.location, jobOpening.location) &&
        Objects.equals(this.countryId, jobOpening.countryId) &&
        Objects.equals(this.employmentType, jobOpening.employmentType) &&
        Objects.equals(this.workMode, jobOpening.workMode) &&
        Objects.equals(this.status, jobOpening.status) &&
        Objects.equals(this.description, jobOpening.description) &&
        Objects.equals(this.companyName, jobOpening.companyName) &&
        Objects.equals(this.companyWebsite, jobOpening.companyWebsite) &&
        Objects.equals(this.companyEmail, jobOpening.companyEmail) &&
        Objects.equals(this.companyLogoFileId, jobOpening.companyLogoFileId) &&
        Objects.equals(this.createTime, jobOpening.createTime) &&
        Objects.equals(this.createdBy, jobOpening.createdBy) &&
        Objects.equals(this.updateTime, jobOpening.updateTime) &&
        Objects.equals(this.updatedBy, jobOpening.updatedBy) &&
        Objects.equals(this.etag, jobOpening.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, postedByUserId, title, location, countryId, employmentType, workMode, status, description, companyName, companyWebsite, companyEmail, companyLogoFileId, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobOpening {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    postedByUserId: ").append(toIndentedString(postedByUserId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    employmentType: ").append(toIndentedString(employmentType)).append("\n");
    sb.append("    workMode: ").append(toIndentedString(workMode)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
    sb.append("    companyWebsite: ").append(toIndentedString(companyWebsite)).append("\n");
    sb.append("    companyEmail: ").append(toIndentedString(companyEmail)).append("\n");
    sb.append("    companyLogoFileId: ").append(toIndentedString(companyLogoFileId)).append("\n");
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

