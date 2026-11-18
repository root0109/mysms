// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of LmsAssignment that can be updated (partial update; see updateMask).
 */

public class LmsAssignmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String question;

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
  private @Nullable SubmissionTypeEnum submissionType;

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

  public LmsAssignmentUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public LmsAssignmentUpdate question(@Nullable String question) {
    this.question = question;
    return this;
  }

  /**
   * Task statement.
   * @return question
   */
  
  @JsonProperty("question")
  public @Nullable String getQuestion() {
    return question;
  }

  @JsonProperty("question")
  public void setQuestion(@Nullable String question) {
    this.question = question;
  }

  public LmsAssignmentUpdate submissionType(@Nullable SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
    return this;
  }

  /**
   * Expected submission type.
   * @return submissionType
   */
  
  @JsonProperty("submissionType")
  public @Nullable SubmissionTypeEnum getSubmissionType() {
    return submissionType;
  }

  @JsonProperty("submissionType")
  public void setSubmissionType(@Nullable SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
  }

  public LmsAssignmentUpdate courseId(@Nullable UUID courseId) {
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

  public LmsAssignmentUpdate isGraded(@Nullable Boolean isGraded) {
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

  public LmsAssignmentUpdate showModelAnswer(@Nullable Boolean showModelAnswer) {
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

  public LmsAssignmentUpdate modelAnswer(@Nullable String modelAnswer) {
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

  public LmsAssignmentUpdate schedulingEnabled(@Nullable Boolean schedulingEnabled) {
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

  public LmsAssignmentUpdate scheduleStart(@Nullable OffsetDateTime scheduleStart) {
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

  public LmsAssignmentUpdate scheduleEnd(@Nullable OffsetDateTime scheduleEnd) {
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

  public LmsAssignmentUpdate institutionId(@Nullable UUID institutionId) {
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

  public LmsAssignmentUpdate submissionMode(@Nullable SubmissionModeEnum submissionMode) {
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

  public LmsAssignmentUpdate minGroupSize(@Nullable Integer minGroupSize) {
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

  public LmsAssignmentUpdate maxGroupSize(@Nullable Integer maxGroupSize) {
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

  public LmsAssignmentUpdate peerReviewPoints(@Nullable String peerReviewPoints) {
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

  public LmsAssignmentUpdate usesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
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

  public LmsAssignmentUpdate isInRepository(@Nullable Boolean isInRepository) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LmsAssignmentUpdate lmsAssignmentUpdate = (LmsAssignmentUpdate) o;
    return Objects.equals(this.title, lmsAssignmentUpdate.title) &&
        Objects.equals(this.question, lmsAssignmentUpdate.question) &&
        Objects.equals(this.submissionType, lmsAssignmentUpdate.submissionType) &&
        Objects.equals(this.courseId, lmsAssignmentUpdate.courseId) &&
        Objects.equals(this.isGraded, lmsAssignmentUpdate.isGraded) &&
        Objects.equals(this.showModelAnswer, lmsAssignmentUpdate.showModelAnswer) &&
        Objects.equals(this.modelAnswer, lmsAssignmentUpdate.modelAnswer) &&
        Objects.equals(this.schedulingEnabled, lmsAssignmentUpdate.schedulingEnabled) &&
        Objects.equals(this.scheduleStart, lmsAssignmentUpdate.scheduleStart) &&
        Objects.equals(this.scheduleEnd, lmsAssignmentUpdate.scheduleEnd) &&
        Objects.equals(this.institutionId, lmsAssignmentUpdate.institutionId) &&
        Objects.equals(this.submissionMode, lmsAssignmentUpdate.submissionMode) &&
        Objects.equals(this.minGroupSize, lmsAssignmentUpdate.minGroupSize) &&
        Objects.equals(this.maxGroupSize, lmsAssignmentUpdate.maxGroupSize) &&
        Objects.equals(this.peerReviewPoints, lmsAssignmentUpdate.peerReviewPoints) &&
        Objects.equals(this.usesInlineAuthoring, lmsAssignmentUpdate.usesInlineAuthoring) &&
        Objects.equals(this.isInRepository, lmsAssignmentUpdate.isInRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, question, submissionType, courseId, isGraded, showModelAnswer, modelAnswer, schedulingEnabled, scheduleStart, scheduleEnd, institutionId, submissionMode, minGroupSize, maxGroupSize, peerReviewPoints, usesInlineAuthoring, isInRepository);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsAssignmentUpdate {\n");
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

