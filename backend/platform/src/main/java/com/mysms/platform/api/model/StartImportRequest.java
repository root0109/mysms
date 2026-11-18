// Owned API contract of platform.
package com.mysms.platform.api.model;

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
 * StartImportRequest
 */

public class StartImportRequest {

  /**
   * What is imported.
   */
  public enum EntityTypeEnum {
    STUDENTS("students"),
    
    GUARDIANS("guardians"),
    
    STAFF("staff"),
    
    LIBRARY_MEDIA("library_media"),
    
    HOLIDAYS("holidays"),
    
    PROMOTIONS("promotions"),
    
    LMS_COURSE_PACKAGE("lms_course_package");

    private final String value;

    EntityTypeEnum(String value) {
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
    public static EntityTypeEnum fromValue(String value) {
      for (EntityTypeEnum b : EntityTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private EntityTypeEnum entityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean dryRun;

  public StartImportRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StartImportRequest(EntityTypeEnum entityType, UUID fileId) {
    this.entityType = entityType;
    this.fileId = fileId;
  }

  public StartImportRequest entityType(EntityTypeEnum entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * What is imported.
   * @return entityType
   */
  @NotNull 
  @JsonProperty("entityType")
  public EntityTypeEnum getEntityType() {
    return entityType;
  }

  @JsonProperty("entityType")
  public void setEntityType(EntityTypeEnum entityType) {
    this.entityType = entityType;
  }

  public StartImportRequest fileId(UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Uploaded file.
   * @return fileId
   */
  @NotNull @Valid 
  @JsonProperty("fileId")
  public UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public StartImportRequest institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Target institution.
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

  public StartImportRequest dryRun(@Nullable Boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /**
   * Validate only.
   * @return dryRun
   */
  
  @JsonProperty("dryRun")
  public @Nullable Boolean getDryRun() {
    return dryRun;
  }

  @JsonProperty("dryRun")
  public void setDryRun(@Nullable Boolean dryRun) {
    this.dryRun = dryRun;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartImportRequest startImportRequest = (StartImportRequest) o;
    return Objects.equals(this.entityType, startImportRequest.entityType) &&
        Objects.equals(this.fileId, startImportRequest.fileId) &&
        Objects.equals(this.institutionId, startImportRequest.institutionId) &&
        Objects.equals(this.dryRun, startImportRequest.dryRun);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, fileId, institutionId, dryRun);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartImportRequest {\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    dryRun: ").append(toIndentedString(dryRun)).append("\n");
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

