// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Learning assignment attached to lessons / cohorts (separate from official school assignments).
 */

public class LmsAssignment {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID supersededById;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String question;

  /**
   * Expected submission type.
   */
  public enum SubmissionTypeEnum {
    DOCUMENT("document"),
    
    PDF("pdf"),
    
    URL("url"),
    
    IMAGE("image"),
    
    TEXT("text");

    private final String value;

    SubmissionTypeEnum(String value) {
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
    public static SubmissionTypeEnum fromValue(String value) {
      for (SubmissionTypeEnum b : SubmissionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SubmissionTypeEnum submissionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showModelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String modelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean schedulingEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scheduleStart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scheduleEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  /**
   * Individual or group work.
   */
  public enum SubmissionModeEnum {
    INDIVIDUAL("individual"),
    
    GROUP("group");

    private final String value;

    SubmissionModeEnum(String value) {
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
    public static SubmissionModeEnum fromValue(String value) {
      for (SubmissionModeEnum b : SubmissionModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable SubmissionModeEnum submissionMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer minGroupSize;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxGroupSize;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String peerReviewPoints;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean usesInlineAuthoring;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isInRepository;

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

  public LmsAssignment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LmsAssignment(String title, String question, SubmissionTypeEnum submissionType) {
    this.title = title;
    this.question = question;
    this.submissionType = submissionType;
  }

  public LmsAssignment version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Version number; published versions are immutable, edits create a new version.
   * @return version
   */
  
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public LmsAssignment supersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
    return this;
  }

  /**
   * Newer version that replaced this one.
   * @return supersededById
   */
  @Valid 
  @JsonProperty("supersededById")
  public @Nullable UUID getSupersededById() {
    return supersededById;
  }

  @JsonProperty("supersededById")
  public void setSupersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
  }

  public LmsAssignment publishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * When this version was published (absent = draft).
   * @return publishedAt
   */
  @Valid 
  @JsonProperty("publishedAt")
  public @Nullable OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  @JsonProperty("publishedAt")
  public void setPublishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public LmsAssignment id(@Nullable UUID id) {
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

  public LmsAssignment title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public LmsAssignment question(String question) {
    this.question = question;
    return this;
  }

  /**
   * Task statement.
   * @return question
   */
  @NotNull 
  @JsonProperty("question")
  public String getQuestion() {
    return question;
  }

  @JsonProperty("question")
  public void setQuestion(String question) {
    this.question = question;
  }

  public LmsAssignment submissionType(SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
    return this;
  }

  /**
   * Expected submission type.
   * @return submissionType
   */
  @NotNull 
  @JsonProperty("submissionType")
  public SubmissionTypeEnum getSubmissionType() {
    return submissionType;
  }

  @JsonProperty("submissionType")
  public void setSubmissionType(SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
  }

  public LmsAssignment courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
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

  public LmsAssignment isGraded(@Nullable Boolean isGraded) {
    this.isGraded = isGraded;
    return this;
  }

  /**
   * Graded by an evaluator.
   * @return isGraded
   */
  
  @JsonProperty("isGraded")
  public @Nullable Boolean getIsGraded() {
    return isGraded;
  }

  @JsonProperty("isGraded")
  public void setIsGraded(@Nullable Boolean isGraded) {
    this.isGraded = isGraded;
  }

  public LmsAssignment showModelAnswer(@Nullable Boolean showModelAnswer) {
    this.showModelAnswer = showModelAnswer;
    return this;
  }

  /**
   * Show the model answer after submission.
   * @return showModelAnswer
   */
  
  @JsonProperty("showModelAnswer")
  public @Nullable Boolean getShowModelAnswer() {
    return showModelAnswer;
  }

  @JsonProperty("showModelAnswer")
  public void setShowModelAnswer(@Nullable Boolean showModelAnswer) {
    this.showModelAnswer = showModelAnswer;
  }

  public LmsAssignment modelAnswer(@Nullable String modelAnswer) {
    this.modelAnswer = modelAnswer;
    return this;
  }

  /**
   * Model answer.
   * @return modelAnswer
   */
  
  @JsonProperty("modelAnswer")
  public @Nullable String getModelAnswer() {
    return modelAnswer;
  }

  @JsonProperty("modelAnswer")
  public void setModelAnswer(@Nullable String modelAnswer) {
    this.modelAnswer = modelAnswer;
  }

  public LmsAssignment schedulingEnabled(@Nullable Boolean schedulingEnabled) {
    this.schedulingEnabled = schedulingEnabled;
    return this;
  }

  /**
   * Only available in a window.
   * @return schedulingEnabled
   */
  
  @JsonProperty("schedulingEnabled")
  public @Nullable Boolean getSchedulingEnabled() {
    return schedulingEnabled;
  }

  @JsonProperty("schedulingEnabled")
  public void setSchedulingEnabled(@Nullable Boolean schedulingEnabled) {
    this.schedulingEnabled = schedulingEnabled;
  }

  public LmsAssignment scheduleStart(@Nullable OffsetDateTime scheduleStart) {
    this.scheduleStart = scheduleStart;
    return this;
  }

  /**
   * Window start.
   * @return scheduleStart
   */
  @Valid 
  @JsonProperty("scheduleStart")
  public @Nullable OffsetDateTime getScheduleStart() {
    return scheduleStart;
  }

  @JsonProperty("scheduleStart")
  public void setScheduleStart(@Nullable OffsetDateTime scheduleStart) {
    this.scheduleStart = scheduleStart;
  }

  public LmsAssignment scheduleEnd(@Nullable OffsetDateTime scheduleEnd) {
    this.scheduleEnd = scheduleEnd;
    return this;
  }

  /**
   * Window end.
   * @return scheduleEnd
   */
  @Valid 
  @JsonProperty("scheduleEnd")
  public @Nullable OffsetDateTime getScheduleEnd() {
    return scheduleEnd;
  }

  @JsonProperty("scheduleEnd")
  public void setScheduleEnd(@Nullable OffsetDateTime scheduleEnd) {
    this.scheduleEnd = scheduleEnd;
  }

  public LmsAssignment institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (repository scope).
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

  public LmsAssignment submissionMode(@Nullable SubmissionModeEnum submissionMode) {
    this.submissionMode = submissionMode;
    return this;
  }

  /**
   * Individual or group work.
   * @return submissionMode
   */
  
  @JsonProperty("submissionMode")
  public @Nullable SubmissionModeEnum getSubmissionMode() {
    return submissionMode;
  }

  @JsonProperty("submissionMode")
  public void setSubmissionMode(@Nullable SubmissionModeEnum submissionMode) {
    this.submissionMode = submissionMode;
  }

  public LmsAssignment minGroupSize(@Nullable Integer minGroupSize) {
    this.minGroupSize = minGroupSize;
    return this;
  }

  /**
   * Minimum group size.
   * @return minGroupSize
   */
  
  @JsonProperty("minGroupSize")
  public @Nullable Integer getMinGroupSize() {
    return minGroupSize;
  }

  @JsonProperty("minGroupSize")
  public void setMinGroupSize(@Nullable Integer minGroupSize) {
    this.minGroupSize = minGroupSize;
  }

  public LmsAssignment maxGroupSize(@Nullable Integer maxGroupSize) {
    this.maxGroupSize = maxGroupSize;
    return this;
  }

  /**
   * Maximum group size.
   * @return maxGroupSize
   */
  
  @JsonProperty("maxGroupSize")
  public @Nullable Integer getMaxGroupSize() {
    return maxGroupSize;
  }

  @JsonProperty("maxGroupSize")
  public void setMaxGroupSize(@Nullable Integer maxGroupSize) {
    this.maxGroupSize = maxGroupSize;
  }

  public LmsAssignment peerReviewPoints(@Nullable String peerReviewPoints) {
    this.peerReviewPoints = peerReviewPoints;
    return this;
  }

  /**
   * Points each member distributes in peer review (NULL = no peer review).
   * @return peerReviewPoints
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("peerReviewPoints")
  public @Nullable String getPeerReviewPoints() {
    return peerReviewPoints;
  }

  @JsonProperty("peerReviewPoints")
  public void setPeerReviewPoints(@Nullable String peerReviewPoints) {
    this.peerReviewPoints = peerReviewPoints;
  }

  public LmsAssignment usesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
    return this;
  }

  /**
   * Task authored with the inline editor (embedded media).
   * @return usesInlineAuthoring
   */
  
  @JsonProperty("usesInlineAuthoring")
  public @Nullable Boolean getUsesInlineAuthoring() {
    return usesInlineAuthoring;
  }

  @JsonProperty("usesInlineAuthoring")
  public void setUsesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
  }

  public LmsAssignment isInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
    return this;
  }

  /**
   * Listed in the shared content repository.
   * @return isInRepository
   */
  
  @JsonProperty("isInRepository")
  public @Nullable Boolean getIsInRepository() {
    return isInRepository;
  }

  @JsonProperty("isInRepository")
  public void setIsInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
  }

  public LmsAssignment createTime(@Nullable OffsetDateTime createTime) {
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

  public LmsAssignment createdBy(@Nullable UUID createdBy) {
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

  public LmsAssignment updateTime(@Nullable OffsetDateTime updateTime) {
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

  public LmsAssignment updatedBy(@Nullable UUID updatedBy) {
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

  public LmsAssignment etag(@Nullable String etag) {
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
    LmsAssignment lmsAssignment = (LmsAssignment) o;
    return Objects.equals(this.version, lmsAssignment.version) &&
        Objects.equals(this.supersededById, lmsAssignment.supersededById) &&
        Objects.equals(this.publishedAt, lmsAssignment.publishedAt) &&
        Objects.equals(this.id, lmsAssignment.id) &&
        Objects.equals(this.title, lmsAssignment.title) &&
        Objects.equals(this.question, lmsAssignment.question) &&
        Objects.equals(this.submissionType, lmsAssignment.submissionType) &&
        Objects.equals(this.courseId, lmsAssignment.courseId) &&
        Objects.equals(this.isGraded, lmsAssignment.isGraded) &&
        Objects.equals(this.showModelAnswer, lmsAssignment.showModelAnswer) &&
        Objects.equals(this.modelAnswer, lmsAssignment.modelAnswer) &&
        Objects.equals(this.schedulingEnabled, lmsAssignment.schedulingEnabled) &&
        Objects.equals(this.scheduleStart, lmsAssignment.scheduleStart) &&
        Objects.equals(this.scheduleEnd, lmsAssignment.scheduleEnd) &&
        Objects.equals(this.institutionId, lmsAssignment.institutionId) &&
        Objects.equals(this.submissionMode, lmsAssignment.submissionMode) &&
        Objects.equals(this.minGroupSize, lmsAssignment.minGroupSize) &&
        Objects.equals(this.maxGroupSize, lmsAssignment.maxGroupSize) &&
        Objects.equals(this.peerReviewPoints, lmsAssignment.peerReviewPoints) &&
        Objects.equals(this.usesInlineAuthoring, lmsAssignment.usesInlineAuthoring) &&
        Objects.equals(this.isInRepository, lmsAssignment.isInRepository) &&
        Objects.equals(this.createTime, lmsAssignment.createTime) &&
        Objects.equals(this.createdBy, lmsAssignment.createdBy) &&
        Objects.equals(this.updateTime, lmsAssignment.updateTime) &&
        Objects.equals(this.updatedBy, lmsAssignment.updatedBy) &&
        Objects.equals(this.etag, lmsAssignment.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, supersededById, publishedAt, id, title, question, submissionType, courseId, isGraded, showModelAnswer, modelAnswer, schedulingEnabled, scheduleStart, scheduleEnd, institutionId, submissionMode, minGroupSize, maxGroupSize, peerReviewPoints, usesInlineAuthoring, isInRepository, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsAssignment {\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    supersededById: ").append(toIndentedString(supersededById)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    question: ").append(toIndentedString(question)).append("\n");
    sb.append("    submissionType: ").append(toIndentedString(submissionType)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    isGraded: ").append(toIndentedString(isGraded)).append("\n");
    sb.append("    showModelAnswer: ").append(toIndentedString(showModelAnswer)).append("\n");
    sb.append("    modelAnswer: ").append(toIndentedString(modelAnswer)).append("\n");
    sb.append("    schedulingEnabled: ").append(toIndentedString(schedulingEnabled)).append("\n");
    sb.append("    scheduleStart: ").append(toIndentedString(scheduleStart)).append("\n");
    sb.append("    scheduleEnd: ").append(toIndentedString(scheduleEnd)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    submissionMode: ").append(toIndentedString(submissionMode)).append("\n");
    sb.append("    minGroupSize: ").append(toIndentedString(minGroupSize)).append("\n");
    sb.append("    maxGroupSize: ").append(toIndentedString(maxGroupSize)).append("\n");
    sb.append("    peerReviewPoints: ").append(toIndentedString(peerReviewPoints)).append("\n");
    sb.append("    usesInlineAuthoring: ").append(toIndentedString(usesInlineAuthoring)).append("\n");
    sb.append("    isInRepository: ").append(toIndentedString(isInRepository)).append("\n");
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

