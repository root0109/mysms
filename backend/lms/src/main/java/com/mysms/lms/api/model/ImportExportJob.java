// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Background import or export of a course or cohort package (native or IMS Common Cartridge).
 */

public class ImportExportJob {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  /**
   * Import or export.
   */
  public enum DirectionEnum {
    IMPORT("import"),
    
    EXPORT("export");

    private final String value;

    DirectionEnum(String value) {
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
    public static DirectionEnum fromValue(String value) {
      for (DirectionEnum b : DirectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private DirectionEnum direction;

  /**
   * Package format.
   */
  public enum PackageFormatEnum {
    NATIVE("native"),
    
    COMMON_CARTRIDGE("common_cartridge"),
    
    COHORT_ARCHIVE("cohort_archive");

    private final String value;

    PackageFormatEnum(String value) {
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
    public static PackageFormatEnum fromValue(String value) {
      for (PackageFormatEnum b : PackageFormatEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private PackageFormatEnum packageFormat;

  /**
   * Whether a course template or a cohort (class) is transferred.
   */
  public enum ScopeEnum {
    COURSE("course"),
    
    COHORT("cohort");

    private final String value;

    ScopeEnum(String value) {
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
    public static ScopeEnum fromValue(String value) {
      for (ScopeEnum b : ScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID targetChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean addToRepository;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID packageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID requestedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate requestedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startedOn;

  /**
   * Job state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    PENDING("pending"),
    
    IN_PROGRESS("in_progress"),
    
    COMPLETE("complete"),
    
    ERROR("error"),
    
    CANCELLED("cancelled");

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

  /**
   * Restore state of an exported cohort archive.
   */
  public enum UnarchiveStatusEnum {
    PENDING("pending"),
    
    IN_PROGRESS("in_progress"),
    
    COMPLETE("complete"),
    
    ERROR("error");

    private final String value;

    UnarchiveStatusEnum(String value) {
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
    public static UnarchiveStatusEnum fromValue(String value) {
      for (UnarchiveStatusEnum b : UnarchiveStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UnarchiveStatusEnum unarchiveStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String errorLog;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String cohortCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate cohortStartDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String courseCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String courseVersion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String courseTitle;

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

  public ImportExportJob() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImportExportJob(DirectionEnum direction, PackageFormatEnum packageFormat, ScopeEnum scope, UUID requestedBy, LocalDate requestedOn) {
    this.direction = direction;
    this.packageFormat = packageFormat;
    this.scope = scope;
    this.requestedBy = requestedBy;
    this.requestedOn = requestedOn;
  }

  public ImportExportJob id(@Nullable UUID id) {
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

  public ImportExportJob institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution the job runs for.
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

  public ImportExportJob direction(DirectionEnum direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Import or export.
   * @return direction
   */
  @NotNull 
  @JsonProperty("direction")
  public DirectionEnum getDirection() {
    return direction;
  }

  @JsonProperty("direction")
  public void setDirection(DirectionEnum direction) {
    this.direction = direction;
  }

  public ImportExportJob packageFormat(PackageFormatEnum packageFormat) {
    this.packageFormat = packageFormat;
    return this;
  }

  /**
   * Package format.
   * @return packageFormat
   */
  @NotNull 
  @JsonProperty("packageFormat")
  public PackageFormatEnum getPackageFormat() {
    return packageFormat;
  }

  @JsonProperty("packageFormat")
  public void setPackageFormat(PackageFormatEnum packageFormat) {
    this.packageFormat = packageFormat;
  }

  public ImportExportJob scope(ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Whether a course template or a cohort (class) is transferred.
   * @return scope
   */
  @NotNull 
  @JsonProperty("scope")
  public ScopeEnum getScope() {
    return scope;
  }

  @JsonProperty("scope")
  public void setScope(ScopeEnum scope) {
    this.scope = scope;
  }

  public ImportExportJob courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course imported into / exported from.
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

  public ImportExportJob cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort imported into / exported from.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ImportExportJob targetChapterId(@Nullable UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
    return this;
  }

  /**
   * Chapter (folder) the imported content is placed in.
   * @return targetChapterId
   */
  @Valid 
  @JsonProperty("targetChapterId")
  public @Nullable UUID getTargetChapterId() {
    return targetChapterId;
  }

  @JsonProperty("targetChapterId")
  public void setTargetChapterId(@Nullable UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
  }

  public ImportExportJob addToRepository(@Nullable Boolean addToRepository) {
    this.addToRepository = addToRepository;
    return this;
  }

  /**
   * Also add imported items to the shared content repository.
   * @return addToRepository
   */
  
  @JsonProperty("addToRepository")
  public @Nullable Boolean getAddToRepository() {
    return addToRepository;
  }

  @JsonProperty("addToRepository")
  public void setAddToRepository(@Nullable Boolean addToRepository) {
    this.addToRepository = addToRepository;
  }

  public ImportExportJob packageFileId(@Nullable UUID packageFileId) {
    this.packageFileId = packageFileId;
    return this;
  }

  /**
   * Package file (uploaded for import, produced by export).
   * @return packageFileId
   */
  @Valid 
  @JsonProperty("packageFileId")
  public @Nullable UUID getPackageFileId() {
    return packageFileId;
  }

  @JsonProperty("packageFileId")
  public void setPackageFileId(@Nullable UUID packageFileId) {
    this.packageFileId = packageFileId;
  }

  public ImportExportJob requestedBy(UUID requestedBy) {
    this.requestedBy = requestedBy;
    return this;
  }

  /**
   * User who requested the job.
   * @return requestedBy
   */
  @NotNull @Valid 
  @JsonProperty("requestedBy")
  public UUID getRequestedBy() {
    return requestedBy;
  }

  @JsonProperty("requestedBy")
  public void setRequestedBy(UUID requestedBy) {
    this.requestedBy = requestedBy;
  }

  public ImportExportJob requestedOn(LocalDate requestedOn) {
    this.requestedOn = requestedOn;
    return this;
  }

  /**
   * Request date.
   * @return requestedOn
   */
  @NotNull @Valid 
  @JsonProperty("requestedOn")
  public LocalDate getRequestedOn() {
    return requestedOn;
  }

  @JsonProperty("requestedOn")
  public void setRequestedOn(LocalDate requestedOn) {
    this.requestedOn = requestedOn;
  }

  public ImportExportJob startedOn(@Nullable LocalDate startedOn) {
    this.startedOn = startedOn;
    return this;
  }

  /**
   * Date the worker started the job.
   * @return startedOn
   */
  @Valid 
  @JsonProperty("startedOn")
  public @Nullable LocalDate getStartedOn() {
    return startedOn;
  }

  @JsonProperty("startedOn")
  public void setStartedOn(@Nullable LocalDate startedOn) {
    this.startedOn = startedOn;
  }

  public ImportExportJob status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Job state. Changed only through the workflow operations.
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

  public ImportExportJob unarchiveStatus(@Nullable UnarchiveStatusEnum unarchiveStatus) {
    this.unarchiveStatus = unarchiveStatus;
    return this;
  }

  /**
   * Restore state of an exported cohort archive.
   * @return unarchiveStatus
   */
  
  @JsonProperty("unarchiveStatus")
  public @Nullable UnarchiveStatusEnum getUnarchiveStatus() {
    return unarchiveStatus;
  }

  @JsonProperty("unarchiveStatus")
  public void setUnarchiveStatus(@Nullable UnarchiveStatusEnum unarchiveStatus) {
    this.unarchiveStatus = unarchiveStatus;
  }

  public ImportExportJob errorLog(@Nullable String errorLog) {
    this.errorLog = errorLog;
    return this;
  }

  /**
   * Error details.
   * @return errorLog
   */
  
  @JsonProperty("errorLog")
  public @Nullable String getErrorLog() {
    return errorLog;
  }

  @JsonProperty("errorLog")
  public void setErrorLog(@Nullable String errorLog) {
    this.errorLog = errorLog;
  }

  public ImportExportJob cohortCode(@Nullable String cohortCode) {
    this.cohortCode = cohortCode;
    return this;
  }

  /**
   * Cohort code recorded in the package.
   * @return cohortCode
   */
  @Size(max = 50) 
  @JsonProperty("cohortCode")
  public @Nullable String getCohortCode() {
    return cohortCode;
  }

  @JsonProperty("cohortCode")
  public void setCohortCode(@Nullable String cohortCode) {
    this.cohortCode = cohortCode;
  }

  public ImportExportJob cohortStartDate(@Nullable LocalDate cohortStartDate) {
    this.cohortStartDate = cohortStartDate;
    return this;
  }

  /**
   * Cohort start date recorded in the package.
   * @return cohortStartDate
   */
  @Valid 
  @JsonProperty("cohortStartDate")
  public @Nullable LocalDate getCohortStartDate() {
    return cohortStartDate;
  }

  @JsonProperty("cohortStartDate")
  public void setCohortStartDate(@Nullable LocalDate cohortStartDate) {
    this.cohortStartDate = cohortStartDate;
  }

  public ImportExportJob courseCode(@Nullable String courseCode) {
    this.courseCode = courseCode;
    return this;
  }

  /**
   * Course code recorded in the package.
   * @return courseCode
   */
  @Size(max = 50) 
  @JsonProperty("courseCode")
  public @Nullable String getCourseCode() {
    return courseCode;
  }

  @JsonProperty("courseCode")
  public void setCourseCode(@Nullable String courseCode) {
    this.courseCode = courseCode;
  }

  public ImportExportJob courseVersion(@Nullable String courseVersion) {
    this.courseVersion = courseVersion;
    return this;
  }

  /**
   * Course version recorded in the package.
   * @return courseVersion
   */
  @Size(max = 20) 
  @JsonProperty("courseVersion")
  public @Nullable String getCourseVersion() {
    return courseVersion;
  }

  @JsonProperty("courseVersion")
  public void setCourseVersion(@Nullable String courseVersion) {
    this.courseVersion = courseVersion;
  }

  public ImportExportJob courseTitle(@Nullable String courseTitle) {
    this.courseTitle = courseTitle;
    return this;
  }

  /**
   * Course title recorded in the package.
   * @return courseTitle
   */
  @Size(max = 200) 
  @JsonProperty("courseTitle")
  public @Nullable String getCourseTitle() {
    return courseTitle;
  }

  @JsonProperty("courseTitle")
  public void setCourseTitle(@Nullable String courseTitle) {
    this.courseTitle = courseTitle;
  }

  public ImportExportJob createTime(@Nullable OffsetDateTime createTime) {
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

  public ImportExportJob createdBy(@Nullable UUID createdBy) {
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

  public ImportExportJob updateTime(@Nullable OffsetDateTime updateTime) {
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

  public ImportExportJob updatedBy(@Nullable UUID updatedBy) {
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

  public ImportExportJob etag(@Nullable String etag) {
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
    ImportExportJob importExportJob = (ImportExportJob) o;
    return Objects.equals(this.id, importExportJob.id) &&
        Objects.equals(this.institutionId, importExportJob.institutionId) &&
        Objects.equals(this.direction, importExportJob.direction) &&
        Objects.equals(this.packageFormat, importExportJob.packageFormat) &&
        Objects.equals(this.scope, importExportJob.scope) &&
        Objects.equals(this.courseId, importExportJob.courseId) &&
        Objects.equals(this.cohortId, importExportJob.cohortId) &&
        Objects.equals(this.targetChapterId, importExportJob.targetChapterId) &&
        Objects.equals(this.addToRepository, importExportJob.addToRepository) &&
        Objects.equals(this.packageFileId, importExportJob.packageFileId) &&
        Objects.equals(this.requestedBy, importExportJob.requestedBy) &&
        Objects.equals(this.requestedOn, importExportJob.requestedOn) &&
        Objects.equals(this.startedOn, importExportJob.startedOn) &&
        Objects.equals(this.status, importExportJob.status) &&
        Objects.equals(this.unarchiveStatus, importExportJob.unarchiveStatus) &&
        Objects.equals(this.errorLog, importExportJob.errorLog) &&
        Objects.equals(this.cohortCode, importExportJob.cohortCode) &&
        Objects.equals(this.cohortStartDate, importExportJob.cohortStartDate) &&
        Objects.equals(this.courseCode, importExportJob.courseCode) &&
        Objects.equals(this.courseVersion, importExportJob.courseVersion) &&
        Objects.equals(this.courseTitle, importExportJob.courseTitle) &&
        Objects.equals(this.createTime, importExportJob.createTime) &&
        Objects.equals(this.createdBy, importExportJob.createdBy) &&
        Objects.equals(this.updateTime, importExportJob.updateTime) &&
        Objects.equals(this.updatedBy, importExportJob.updatedBy) &&
        Objects.equals(this.etag, importExportJob.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, institutionId, direction, packageFormat, scope, courseId, cohortId, targetChapterId, addToRepository, packageFileId, requestedBy, requestedOn, startedOn, status, unarchiveStatus, errorLog, cohortCode, cohortStartDate, courseCode, courseVersion, courseTitle, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportExportJob {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
    sb.append("    packageFormat: ").append(toIndentedString(packageFormat)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    targetChapterId: ").append(toIndentedString(targetChapterId)).append("\n");
    sb.append("    addToRepository: ").append(toIndentedString(addToRepository)).append("\n");
    sb.append("    packageFileId: ").append(toIndentedString(packageFileId)).append("\n");
    sb.append("    requestedBy: ").append(toIndentedString(requestedBy)).append("\n");
    sb.append("    requestedOn: ").append(toIndentedString(requestedOn)).append("\n");
    sb.append("    startedOn: ").append(toIndentedString(startedOn)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    unarchiveStatus: ").append(toIndentedString(unarchiveStatus)).append("\n");
    sb.append("    errorLog: ").append(toIndentedString(errorLog)).append("\n");
    sb.append("    cohortCode: ").append(toIndentedString(cohortCode)).append("\n");
    sb.append("    cohortStartDate: ").append(toIndentedString(cohortStartDate)).append("\n");
    sb.append("    courseCode: ").append(toIndentedString(courseCode)).append("\n");
    sb.append("    courseVersion: ").append(toIndentedString(courseVersion)).append("\n");
    sb.append("    courseTitle: ").append(toIndentedString(courseTitle)).append("\n");
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

