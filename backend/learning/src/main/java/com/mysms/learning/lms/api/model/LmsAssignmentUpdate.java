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
 * Fields of LmsAssignment that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LmsAssignmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> question;

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
  private @Nullable Optional<@NotNull SubmissionTypeEnum> submissionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showModelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> modelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> schedulingEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> scheduleStart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> scheduleEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

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
  private @Nullable Optional<@NotNull SubmissionModeEnum> submissionMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> minGroupSize;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxGroupSize;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> peerReviewPoints;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> usesInlineAuthoring;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isInRepository;

  public LmsAssignmentUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public LmsAssignmentUpdate question(@Nullable Optional<String> question) {
    this.question = question;
    return this;
  }

  /**
   * Task statement.
   * @return question
   */
  
  @JsonProperty("question")
  public @Nullable Optional<String> getQuestion() {
    return question;
  }

  @JsonProperty("question")
  public void setQuestion(@Nullable Optional<String> question) {
    this.question = question;
  }

  public LmsAssignmentUpdate submissionType(@Nullable Optional<SubmissionTypeEnum> submissionType) {
    this.submissionType = submissionType;
    return this;
  }

  /**
   * Expected submission type.
   * @return submissionType
   */
  
  @JsonProperty("submissionType")
  public @Nullable Optional<SubmissionTypeEnum> getSubmissionType() {
    return submissionType;
  }

  @JsonProperty("submissionType")
  public void setSubmissionType(@Nullable Optional<SubmissionTypeEnum> submissionType) {
    this.submissionType = submissionType;
  }

  public LmsAssignmentUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public LmsAssignmentUpdate isGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
    return this;
  }

  /**
   * Graded by an evaluator.
   * @return isGraded
   */
  
  @JsonProperty("isGraded")
  public @Nullable Optional<Boolean> getIsGraded() {
    return isGraded;
  }

  @JsonProperty("isGraded")
  public void setIsGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
  }

  public LmsAssignmentUpdate showModelAnswer(@Nullable Optional<Boolean> showModelAnswer) {
    this.showModelAnswer = showModelAnswer;
    return this;
  }

  /**
   * Show the model answer after submission.
   * @return showModelAnswer
   */
  
  @JsonProperty("showModelAnswer")
  public @Nullable Optional<Boolean> getShowModelAnswer() {
    return showModelAnswer;
  }

  @JsonProperty("showModelAnswer")
  public void setShowModelAnswer(@Nullable Optional<Boolean> showModelAnswer) {
    this.showModelAnswer = showModelAnswer;
  }

  public LmsAssignmentUpdate modelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
    return this;
  }

  /**
   * Model answer.
   * @return modelAnswer
   */
  
  @JsonProperty("modelAnswer")
  public @Nullable Optional<String> getModelAnswer() {
    return modelAnswer;
  }

  @JsonProperty("modelAnswer")
  public void setModelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
  }

  public LmsAssignmentUpdate schedulingEnabled(@Nullable Optional<Boolean> schedulingEnabled) {
    this.schedulingEnabled = schedulingEnabled;
    return this;
  }

  /**
   * Only available in a window.
   * @return schedulingEnabled
   */
  
  @JsonProperty("schedulingEnabled")
  public @Nullable Optional<Boolean> getSchedulingEnabled() {
    return schedulingEnabled;
  }

  @JsonProperty("schedulingEnabled")
  public void setSchedulingEnabled(@Nullable Optional<Boolean> schedulingEnabled) {
    this.schedulingEnabled = schedulingEnabled;
  }

  public LmsAssignmentUpdate scheduleStart(@Nullable Optional<OffsetDateTime> scheduleStart) {
    this.scheduleStart = scheduleStart;
    return this;
  }

  /**
   * Window start.
   * @return scheduleStart
   */
  
  @JsonProperty("scheduleStart")
  public @Nullable Optional<OffsetDateTime> getScheduleStart() {
    return scheduleStart;
  }

  @JsonProperty("scheduleStart")
  public void setScheduleStart(@Nullable Optional<OffsetDateTime> scheduleStart) {
    this.scheduleStart = scheduleStart;
  }

  public LmsAssignmentUpdate scheduleEnd(@Nullable Optional<OffsetDateTime> scheduleEnd) {
    this.scheduleEnd = scheduleEnd;
    return this;
  }

  /**
   * Window end.
   * @return scheduleEnd
   */
  
  @JsonProperty("scheduleEnd")
  public @Nullable Optional<OffsetDateTime> getScheduleEnd() {
    return scheduleEnd;
  }

  @JsonProperty("scheduleEnd")
  public void setScheduleEnd(@Nullable Optional<OffsetDateTime> scheduleEnd) {
    this.scheduleEnd = scheduleEnd;
  }

  public LmsAssignmentUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (repository scope).
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public LmsAssignmentUpdate submissionMode(@Nullable Optional<SubmissionModeEnum> submissionMode) {
    this.submissionMode = submissionMode;
    return this;
  }

  /**
   * Individual or group work.
   * @return submissionMode
   */
  
  @JsonProperty("submissionMode")
  public @Nullable Optional<SubmissionModeEnum> getSubmissionMode() {
    return submissionMode;
  }

  @JsonProperty("submissionMode")
  public void setSubmissionMode(@Nullable Optional<SubmissionModeEnum> submissionMode) {
    this.submissionMode = submissionMode;
  }

  public LmsAssignmentUpdate minGroupSize(@Nullable Optional<Integer> minGroupSize) {
    this.minGroupSize = minGroupSize;
    return this;
  }

  /**
   * Minimum group size.
   * @return minGroupSize
   */
  
  @JsonProperty("minGroupSize")
  public @Nullable Optional<Integer> getMinGroupSize() {
    return minGroupSize;
  }

  @JsonProperty("minGroupSize")
  public void setMinGroupSize(@Nullable Optional<Integer> minGroupSize) {
    this.minGroupSize = minGroupSize;
  }

  public LmsAssignmentUpdate maxGroupSize(@Nullable Optional<Integer> maxGroupSize) {
    this.maxGroupSize = maxGroupSize;
    return this;
  }

  /**
   * Maximum group size.
   * @return maxGroupSize
   */
  
  @JsonProperty("maxGroupSize")
  public @Nullable Optional<Integer> getMaxGroupSize() {
    return maxGroupSize;
  }

  @JsonProperty("maxGroupSize")
  public void setMaxGroupSize(@Nullable Optional<Integer> maxGroupSize) {
    this.maxGroupSize = maxGroupSize;
  }

  public LmsAssignmentUpdate peerReviewPoints(@Nullable Optional<String> peerReviewPoints) {
    this.peerReviewPoints = peerReviewPoints;
    return this;
  }

  /**
   * Points each member distributes in peer review (NULL = no peer review).
   * @return peerReviewPoints
   */
  
  @JsonProperty("peerReviewPoints")
  public @Nullable Optional<String> getPeerReviewPoints() {
    return peerReviewPoints;
  }

  @JsonProperty("peerReviewPoints")
  public void setPeerReviewPoints(@Nullable Optional<String> peerReviewPoints) {
    this.peerReviewPoints = peerReviewPoints;
  }

  public LmsAssignmentUpdate usesInlineAuthoring(@Nullable Optional<Boolean> usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
    return this;
  }

  /**
   * Task authored with the inline editor (embedded media).
   * @return usesInlineAuthoring
   */
  
  @JsonProperty("usesInlineAuthoring")
  public @Nullable Optional<Boolean> getUsesInlineAuthoring() {
    return usesInlineAuthoring;
  }

  @JsonProperty("usesInlineAuthoring")
  public void setUsesInlineAuthoring(@Nullable Optional<Boolean> usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
  }

  public LmsAssignmentUpdate isInRepository(@Nullable Optional<Boolean> isInRepository) {
    this.isInRepository = isInRepository;
    return this;
  }

  /**
   * Listed in the shared content repository.
   * @return isInRepository
   */
  
  @JsonProperty("isInRepository")
  public @Nullable Optional<Boolean> getIsInRepository() {
    return isInRepository;
  }

  @JsonProperty("isInRepository")
  public void setIsInRepository(@Nullable Optional<Boolean> isInRepository) {
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

