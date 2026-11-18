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
 * Quiz attachable to lessons and cohorts.
 */

public class Quiz {

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
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer passingPercentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String totalMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showAnswers;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showSubmissionHistory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean shuffleQuestions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer limitQuestionsTo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean proctoringEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxViolations;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean negativeMarkingEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String negativeMarks;

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isAutoGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isAdaptive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean equalQuestionWeighting;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String marksPerQuestion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowPause;

  /**
   * Which attempt counts when several are allowed.
   */
  public enum AttemptScoringEnum {
    HIGHEST("highest"),
    
    LAST("last"),
    
    AVERAGE("average");

    private final String value;

    AttemptScoringEnum(String value) {
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
    public static AttemptScoringEnum fromValue(String value) {
      for (AttemptScoringEnum b : AttemptScoringEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AttemptScoringEnum attemptScoring;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean dropLowestAttempt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean drawsFromPool;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showFeedback;

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

  public Quiz() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Quiz(String title, Integer passingPercentage, String totalMarks) {
    this.title = title;
    this.passingPercentage = passingPercentage;
    this.totalMarks = totalMarks;
  }

  public Quiz version(@Nullable Integer version) {
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

  public Quiz supersededById(@Nullable UUID supersededById) {
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

  public Quiz publishedAt(@Nullable OffsetDateTime publishedAt) {
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

  public Quiz id(@Nullable UUID id) {
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

  public Quiz title(String title) {
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

  public Quiz courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course the quiz belongs to.
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

  public Quiz lessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson the quiz belongs to.
   * @return lessonId
   */
  @Valid 
  @JsonProperty("lessonId")
  public @Nullable UUID getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
  }

  public Quiz maxAttempts(@Nullable Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  /**
   * Attempt limit (NULL/0 = unlimited).
   * @return maxAttempts
   */
  
  @JsonProperty("maxAttempts")
  public @Nullable Integer getMaxAttempts() {
    return maxAttempts;
  }

  @JsonProperty("maxAttempts")
  public void setMaxAttempts(@Nullable Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Quiz passingPercentage(Integer passingPercentage) {
    this.passingPercentage = passingPercentage;
    return this;
  }

  /**
   * Pass mark (%).
   * @return passingPercentage
   */
  @NotNull 
  @JsonProperty("passingPercentage")
  public Integer getPassingPercentage() {
    return passingPercentage;
  }

  @JsonProperty("passingPercentage")
  public void setPassingPercentage(Integer passingPercentage) {
    this.passingPercentage = passingPercentage;
  }

  public Quiz totalMarks(String totalMarks) {
    this.totalMarks = totalMarks;
    return this;
  }

  /**
   * Total marks.
   * @return totalMarks
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalMarks")
  public String getTotalMarks() {
    return totalMarks;
  }

  @JsonProperty("totalMarks")
  public void setTotalMarks(String totalMarks) {
    this.totalMarks = totalMarks;
  }

  public Quiz showAnswers(@Nullable Boolean showAnswers) {
    this.showAnswers = showAnswers;
    return this;
  }

  /**
   * Show correct answers after submission.
   * @return showAnswers
   */
  
  @JsonProperty("showAnswers")
  public @Nullable Boolean getShowAnswers() {
    return showAnswers;
  }

  @JsonProperty("showAnswers")
  public void setShowAnswers(@Nullable Boolean showAnswers) {
    this.showAnswers = showAnswers;
  }

  public Quiz showSubmissionHistory(@Nullable Boolean showSubmissionHistory) {
    this.showSubmissionHistory = showSubmissionHistory;
    return this;
  }

  /**
   * Show previous attempts.
   * @return showSubmissionHistory
   */
  
  @JsonProperty("showSubmissionHistory")
  public @Nullable Boolean getShowSubmissionHistory() {
    return showSubmissionHistory;
  }

  @JsonProperty("showSubmissionHistory")
  public void setShowSubmissionHistory(@Nullable Boolean showSubmissionHistory) {
    this.showSubmissionHistory = showSubmissionHistory;
  }

  public Quiz shuffleQuestions(@Nullable Boolean shuffleQuestions) {
    this.shuffleQuestions = shuffleQuestions;
    return this;
  }

  /**
   * Randomise question order.
   * @return shuffleQuestions
   */
  
  @JsonProperty("shuffleQuestions")
  public @Nullable Boolean getShuffleQuestions() {
    return shuffleQuestions;
  }

  @JsonProperty("shuffleQuestions")
  public void setShuffleQuestions(@Nullable Boolean shuffleQuestions) {
    this.shuffleQuestions = shuffleQuestions;
  }

  public Quiz limitQuestionsTo(@Nullable Integer limitQuestionsTo) {
    this.limitQuestionsTo = limitQuestionsTo;
    return this;
  }

  /**
   * Questions drawn per attempt.
   * @return limitQuestionsTo
   */
  
  @JsonProperty("limitQuestionsTo")
  public @Nullable Integer getLimitQuestionsTo() {
    return limitQuestionsTo;
  }

  @JsonProperty("limitQuestionsTo")
  public void setLimitQuestionsTo(@Nullable Integer limitQuestionsTo) {
    this.limitQuestionsTo = limitQuestionsTo;
  }

  public Quiz durationMinutes(@Nullable Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Time limit.
   * @return durationMinutes
   */
  
  @JsonProperty("durationMinutes")
  public @Nullable Integer getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(@Nullable Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public Quiz proctoringEnabled(@Nullable Boolean proctoringEnabled) {
    this.proctoringEnabled = proctoringEnabled;
    return this;
  }

  /**
   * Webcam / focus proctoring.
   * @return proctoringEnabled
   */
  
  @JsonProperty("proctoringEnabled")
  public @Nullable Boolean getProctoringEnabled() {
    return proctoringEnabled;
  }

  @JsonProperty("proctoringEnabled")
  public void setProctoringEnabled(@Nullable Boolean proctoringEnabled) {
    this.proctoringEnabled = proctoringEnabled;
  }

  public Quiz maxViolations(@Nullable Integer maxViolations) {
    this.maxViolations = maxViolations;
    return this;
  }

  /**
   * Violations allowed before auto-submit.
   * @return maxViolations
   */
  
  @JsonProperty("maxViolations")
  public @Nullable Integer getMaxViolations() {
    return maxViolations;
  }

  @JsonProperty("maxViolations")
  public void setMaxViolations(@Nullable Integer maxViolations) {
    this.maxViolations = maxViolations;
  }

  public Quiz negativeMarkingEnabled(@Nullable Boolean negativeMarkingEnabled) {
    this.negativeMarkingEnabled = negativeMarkingEnabled;
    return this;
  }

  /**
   * Deduct marks for wrong answers.
   * @return negativeMarkingEnabled
   */
  
  @JsonProperty("negativeMarkingEnabled")
  public @Nullable Boolean getNegativeMarkingEnabled() {
    return negativeMarkingEnabled;
  }

  @JsonProperty("negativeMarkingEnabled")
  public void setNegativeMarkingEnabled(@Nullable Boolean negativeMarkingEnabled) {
    this.negativeMarkingEnabled = negativeMarkingEnabled;
  }

  public Quiz negativeMarks(@Nullable String negativeMarks) {
    this.negativeMarks = negativeMarks;
    return this;
  }

  /**
   * Marks deducted per wrong answer.
   * @return negativeMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("negativeMarks")
  public @Nullable String getNegativeMarks() {
    return negativeMarks;
  }

  @JsonProperty("negativeMarks")
  public void setNegativeMarks(@Nullable String negativeMarks) {
    this.negativeMarks = negativeMarks;
  }

  public Quiz schedulingEnabled(@Nullable Boolean schedulingEnabled) {
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

  public Quiz scheduleStart(@Nullable OffsetDateTime scheduleStart) {
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

  public Quiz scheduleEnd(@Nullable OffsetDateTime scheduleEnd) {
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

  public Quiz institutionId(@Nullable UUID institutionId) {
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

  public Quiz description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Instructions.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public Quiz isAutoGraded(@Nullable Boolean isAutoGraded) {
    this.isAutoGraded = isAutoGraded;
    return this;
  }

  /**
   * Marked automatically (false = needs manual marking).
   * @return isAutoGraded
   */
  
  @JsonProperty("isAutoGraded")
  public @Nullable Boolean getIsAutoGraded() {
    return isAutoGraded;
  }

  @JsonProperty("isAutoGraded")
  public void setIsAutoGraded(@Nullable Boolean isAutoGraded) {
    this.isAutoGraded = isAutoGraded;
  }

  public Quiz isAdaptive(@Nullable Boolean isAdaptive) {
    this.isAdaptive = isAdaptive;
    return this;
  }

  /**
   * Adaptive question selection.
   * @return isAdaptive
   */
  
  @JsonProperty("isAdaptive")
  public @Nullable Boolean getIsAdaptive() {
    return isAdaptive;
  }

  @JsonProperty("isAdaptive")
  public void setIsAdaptive(@Nullable Boolean isAdaptive) {
    this.isAdaptive = isAdaptive;
  }

  public Quiz equalQuestionWeighting(@Nullable Boolean equalQuestionWeighting) {
    this.equalQuestionWeighting = equalQuestionWeighting;
    return this;
  }

  /**
   * Every question carries the same marks.
   * @return equalQuestionWeighting
   */
  
  @JsonProperty("equalQuestionWeighting")
  public @Nullable Boolean getEqualQuestionWeighting() {
    return equalQuestionWeighting;
  }

  @JsonProperty("equalQuestionWeighting")
  public void setEqualQuestionWeighting(@Nullable Boolean equalQuestionWeighting) {
    this.equalQuestionWeighting = equalQuestionWeighting;
  }

  public Quiz marksPerQuestion(@Nullable String marksPerQuestion) {
    this.marksPerQuestion = marksPerQuestion;
    return this;
  }

  /**
   * Marks per question when weighting is equal.
   * @return marksPerQuestion
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marksPerQuestion")
  public @Nullable String getMarksPerQuestion() {
    return marksPerQuestion;
  }

  @JsonProperty("marksPerQuestion")
  public void setMarksPerQuestion(@Nullable String marksPerQuestion) {
    this.marksPerQuestion = marksPerQuestion;
  }

  public Quiz allowPause(@Nullable Boolean allowPause) {
    this.allowPause = allowPause;
    return this;
  }

  /**
   * Students may pause and resume an attempt.
   * @return allowPause
   */
  
  @JsonProperty("allowPause")
  public @Nullable Boolean getAllowPause() {
    return allowPause;
  }

  @JsonProperty("allowPause")
  public void setAllowPause(@Nullable Boolean allowPause) {
    this.allowPause = allowPause;
  }

  public Quiz attemptScoring(@Nullable AttemptScoringEnum attemptScoring) {
    this.attemptScoring = attemptScoring;
    return this;
  }

  /**
   * Which attempt counts when several are allowed.
   * @return attemptScoring
   */
  
  @JsonProperty("attemptScoring")
  public @Nullable AttemptScoringEnum getAttemptScoring() {
    return attemptScoring;
  }

  @JsonProperty("attemptScoring")
  public void setAttemptScoring(@Nullable AttemptScoringEnum attemptScoring) {
    this.attemptScoring = attemptScoring;
  }

  public Quiz dropLowestAttempt(@Nullable Boolean dropLowestAttempt) {
    this.dropLowestAttempt = dropLowestAttempt;
    return this;
  }

  /**
   * Ignore the lowest attempt when averaging.
   * @return dropLowestAttempt
   */
  
  @JsonProperty("dropLowestAttempt")
  public @Nullable Boolean getDropLowestAttempt() {
    return dropLowestAttempt;
  }

  @JsonProperty("dropLowestAttempt")
  public void setDropLowestAttempt(@Nullable Boolean dropLowestAttempt) {
    this.dropLowestAttempt = dropLowestAttempt;
  }

  public Quiz drawsFromPool(@Nullable Boolean drawsFromPool) {
    this.drawsFromPool = drawsFromPool;
    return this;
  }

  /**
   * Questions are drawn at random from question pools.
   * @return drawsFromPool
   */
  
  @JsonProperty("drawsFromPool")
  public @Nullable Boolean getDrawsFromPool() {
    return drawsFromPool;
  }

  @JsonProperty("drawsFromPool")
  public void setDrawsFromPool(@Nullable Boolean drawsFromPool) {
    this.drawsFromPool = drawsFromPool;
  }

  public Quiz showFeedback(@Nullable Boolean showFeedback) {
    this.showFeedback = showFeedback;
    return this;
  }

  /**
   * Show per-question feedback after submission.
   * @return showFeedback
   */
  
  @JsonProperty("showFeedback")
  public @Nullable Boolean getShowFeedback() {
    return showFeedback;
  }

  @JsonProperty("showFeedback")
  public void setShowFeedback(@Nullable Boolean showFeedback) {
    this.showFeedback = showFeedback;
  }

  public Quiz isInRepository(@Nullable Boolean isInRepository) {
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

  public Quiz createTime(@Nullable OffsetDateTime createTime) {
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

  public Quiz createdBy(@Nullable UUID createdBy) {
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

  public Quiz updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Quiz updatedBy(@Nullable UUID updatedBy) {
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

  public Quiz etag(@Nullable String etag) {
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
    Quiz quiz = (Quiz) o;
    return Objects.equals(this.version, quiz.version) &&
        Objects.equals(this.supersededById, quiz.supersededById) &&
        Objects.equals(this.publishedAt, quiz.publishedAt) &&
        Objects.equals(this.id, quiz.id) &&
        Objects.equals(this.title, quiz.title) &&
        Objects.equals(this.courseId, quiz.courseId) &&
        Objects.equals(this.lessonId, quiz.lessonId) &&
        Objects.equals(this.maxAttempts, quiz.maxAttempts) &&
        Objects.equals(this.passingPercentage, quiz.passingPercentage) &&
        Objects.equals(this.totalMarks, quiz.totalMarks) &&
        Objects.equals(this.showAnswers, quiz.showAnswers) &&
        Objects.equals(this.showSubmissionHistory, quiz.showSubmissionHistory) &&
        Objects.equals(this.shuffleQuestions, quiz.shuffleQuestions) &&
        Objects.equals(this.limitQuestionsTo, quiz.limitQuestionsTo) &&
        Objects.equals(this.durationMinutes, quiz.durationMinutes) &&
        Objects.equals(this.proctoringEnabled, quiz.proctoringEnabled) &&
        Objects.equals(this.maxViolations, quiz.maxViolations) &&
        Objects.equals(this.negativeMarkingEnabled, quiz.negativeMarkingEnabled) &&
        Objects.equals(this.negativeMarks, quiz.negativeMarks) &&
        Objects.equals(this.schedulingEnabled, quiz.schedulingEnabled) &&
        Objects.equals(this.scheduleStart, quiz.scheduleStart) &&
        Objects.equals(this.scheduleEnd, quiz.scheduleEnd) &&
        Objects.equals(this.institutionId, quiz.institutionId) &&
        Objects.equals(this.description, quiz.description) &&
        Objects.equals(this.isAutoGraded, quiz.isAutoGraded) &&
        Objects.equals(this.isAdaptive, quiz.isAdaptive) &&
        Objects.equals(this.equalQuestionWeighting, quiz.equalQuestionWeighting) &&
        Objects.equals(this.marksPerQuestion, quiz.marksPerQuestion) &&
        Objects.equals(this.allowPause, quiz.allowPause) &&
        Objects.equals(this.attemptScoring, quiz.attemptScoring) &&
        Objects.equals(this.dropLowestAttempt, quiz.dropLowestAttempt) &&
        Objects.equals(this.drawsFromPool, quiz.drawsFromPool) &&
        Objects.equals(this.showFeedback, quiz.showFeedback) &&
        Objects.equals(this.isInRepository, quiz.isInRepository) &&
        Objects.equals(this.createTime, quiz.createTime) &&
        Objects.equals(this.createdBy, quiz.createdBy) &&
        Objects.equals(this.updateTime, quiz.updateTime) &&
        Objects.equals(this.updatedBy, quiz.updatedBy) &&
        Objects.equals(this.etag, quiz.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, supersededById, publishedAt, id, title, courseId, lessonId, maxAttempts, passingPercentage, totalMarks, showAnswers, showSubmissionHistory, shuffleQuestions, limitQuestionsTo, durationMinutes, proctoringEnabled, maxViolations, negativeMarkingEnabled, negativeMarks, schedulingEnabled, scheduleStart, scheduleEnd, institutionId, description, isAutoGraded, isAdaptive, equalQuestionWeighting, marksPerQuestion, allowPause, attemptScoring, dropLowestAttempt, drawsFromPool, showFeedback, isInRepository, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Quiz {\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    supersededById: ").append(toIndentedString(supersededById)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    maxAttempts: ").append(toIndentedString(maxAttempts)).append("\n");
    sb.append("    passingPercentage: ").append(toIndentedString(passingPercentage)).append("\n");
    sb.append("    totalMarks: ").append(toIndentedString(totalMarks)).append("\n");
    sb.append("    showAnswers: ").append(toIndentedString(showAnswers)).append("\n");
    sb.append("    showSubmissionHistory: ").append(toIndentedString(showSubmissionHistory)).append("\n");
    sb.append("    shuffleQuestions: ").append(toIndentedString(shuffleQuestions)).append("\n");
    sb.append("    limitQuestionsTo: ").append(toIndentedString(limitQuestionsTo)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    proctoringEnabled: ").append(toIndentedString(proctoringEnabled)).append("\n");
    sb.append("    maxViolations: ").append(toIndentedString(maxViolations)).append("\n");
    sb.append("    negativeMarkingEnabled: ").append(toIndentedString(negativeMarkingEnabled)).append("\n");
    sb.append("    negativeMarks: ").append(toIndentedString(negativeMarks)).append("\n");
    sb.append("    schedulingEnabled: ").append(toIndentedString(schedulingEnabled)).append("\n");
    sb.append("    scheduleStart: ").append(toIndentedString(scheduleStart)).append("\n");
    sb.append("    scheduleEnd: ").append(toIndentedString(scheduleEnd)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isAutoGraded: ").append(toIndentedString(isAutoGraded)).append("\n");
    sb.append("    isAdaptive: ").append(toIndentedString(isAdaptive)).append("\n");
    sb.append("    equalQuestionWeighting: ").append(toIndentedString(equalQuestionWeighting)).append("\n");
    sb.append("    marksPerQuestion: ").append(toIndentedString(marksPerQuestion)).append("\n");
    sb.append("    allowPause: ").append(toIndentedString(allowPause)).append("\n");
    sb.append("    attemptScoring: ").append(toIndentedString(attemptScoring)).append("\n");
    sb.append("    dropLowestAttempt: ").append(toIndentedString(dropLowestAttempt)).append("\n");
    sb.append("    drawsFromPool: ").append(toIndentedString(drawsFromPool)).append("\n");
    sb.append("    showFeedback: ").append(toIndentedString(showFeedback)).append("\n");
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

