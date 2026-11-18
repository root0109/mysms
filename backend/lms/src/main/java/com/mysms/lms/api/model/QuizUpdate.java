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
 * Fields of Quiz that can be updated (partial update; see updateMask).
 */

public class QuizUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer passingPercentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String totalMarks;

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

  public QuizUpdate title(@Nullable String title) {
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

  public QuizUpdate courseId(@Nullable UUID courseId) {
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

  public QuizUpdate lessonId(@Nullable UUID lessonId) {
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

  public QuizUpdate maxAttempts(@Nullable Integer maxAttempts) {
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

  public QuizUpdate passingPercentage(@Nullable Integer passingPercentage) {
    this.passingPercentage = passingPercentage;
    return this;
  }

  /**
   * Pass mark (%).
   * @return passingPercentage
   */
  
  @JsonProperty("passingPercentage")
  public @Nullable Integer getPassingPercentage() {
    return passingPercentage;
  }

  @JsonProperty("passingPercentage")
  public void setPassingPercentage(@Nullable Integer passingPercentage) {
    this.passingPercentage = passingPercentage;
  }

  public QuizUpdate totalMarks(@Nullable String totalMarks) {
    this.totalMarks = totalMarks;
    return this;
  }

  /**
   * Total marks.
   * @return totalMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalMarks")
  public @Nullable String getTotalMarks() {
    return totalMarks;
  }

  @JsonProperty("totalMarks")
  public void setTotalMarks(@Nullable String totalMarks) {
    this.totalMarks = totalMarks;
  }

  public QuizUpdate showAnswers(@Nullable Boolean showAnswers) {
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

  public QuizUpdate showSubmissionHistory(@Nullable Boolean showSubmissionHistory) {
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

  public QuizUpdate shuffleQuestions(@Nullable Boolean shuffleQuestions) {
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

  public QuizUpdate limitQuestionsTo(@Nullable Integer limitQuestionsTo) {
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

  public QuizUpdate durationMinutes(@Nullable Integer durationMinutes) {
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

  public QuizUpdate proctoringEnabled(@Nullable Boolean proctoringEnabled) {
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

  public QuizUpdate maxViolations(@Nullable Integer maxViolations) {
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

  public QuizUpdate negativeMarkingEnabled(@Nullable Boolean negativeMarkingEnabled) {
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

  public QuizUpdate negativeMarks(@Nullable String negativeMarks) {
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

  public QuizUpdate schedulingEnabled(@Nullable Boolean schedulingEnabled) {
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

  public QuizUpdate scheduleStart(@Nullable OffsetDateTime scheduleStart) {
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

  public QuizUpdate scheduleEnd(@Nullable OffsetDateTime scheduleEnd) {
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

  public QuizUpdate institutionId(@Nullable UUID institutionId) {
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

  public QuizUpdate description(@Nullable String description) {
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

  public QuizUpdate isAutoGraded(@Nullable Boolean isAutoGraded) {
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

  public QuizUpdate isAdaptive(@Nullable Boolean isAdaptive) {
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

  public QuizUpdate equalQuestionWeighting(@Nullable Boolean equalQuestionWeighting) {
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

  public QuizUpdate marksPerQuestion(@Nullable String marksPerQuestion) {
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

  public QuizUpdate allowPause(@Nullable Boolean allowPause) {
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

  public QuizUpdate attemptScoring(@Nullable AttemptScoringEnum attemptScoring) {
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

  public QuizUpdate dropLowestAttempt(@Nullable Boolean dropLowestAttempt) {
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

  public QuizUpdate drawsFromPool(@Nullable Boolean drawsFromPool) {
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

  public QuizUpdate showFeedback(@Nullable Boolean showFeedback) {
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

  public QuizUpdate isInRepository(@Nullable Boolean isInRepository) {
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
    QuizUpdate quizUpdate = (QuizUpdate) o;
    return Objects.equals(this.title, quizUpdate.title) &&
        Objects.equals(this.courseId, quizUpdate.courseId) &&
        Objects.equals(this.lessonId, quizUpdate.lessonId) &&
        Objects.equals(this.maxAttempts, quizUpdate.maxAttempts) &&
        Objects.equals(this.passingPercentage, quizUpdate.passingPercentage) &&
        Objects.equals(this.totalMarks, quizUpdate.totalMarks) &&
        Objects.equals(this.showAnswers, quizUpdate.showAnswers) &&
        Objects.equals(this.showSubmissionHistory, quizUpdate.showSubmissionHistory) &&
        Objects.equals(this.shuffleQuestions, quizUpdate.shuffleQuestions) &&
        Objects.equals(this.limitQuestionsTo, quizUpdate.limitQuestionsTo) &&
        Objects.equals(this.durationMinutes, quizUpdate.durationMinutes) &&
        Objects.equals(this.proctoringEnabled, quizUpdate.proctoringEnabled) &&
        Objects.equals(this.maxViolations, quizUpdate.maxViolations) &&
        Objects.equals(this.negativeMarkingEnabled, quizUpdate.negativeMarkingEnabled) &&
        Objects.equals(this.negativeMarks, quizUpdate.negativeMarks) &&
        Objects.equals(this.schedulingEnabled, quizUpdate.schedulingEnabled) &&
        Objects.equals(this.scheduleStart, quizUpdate.scheduleStart) &&
        Objects.equals(this.scheduleEnd, quizUpdate.scheduleEnd) &&
        Objects.equals(this.institutionId, quizUpdate.institutionId) &&
        Objects.equals(this.description, quizUpdate.description) &&
        Objects.equals(this.isAutoGraded, quizUpdate.isAutoGraded) &&
        Objects.equals(this.isAdaptive, quizUpdate.isAdaptive) &&
        Objects.equals(this.equalQuestionWeighting, quizUpdate.equalQuestionWeighting) &&
        Objects.equals(this.marksPerQuestion, quizUpdate.marksPerQuestion) &&
        Objects.equals(this.allowPause, quizUpdate.allowPause) &&
        Objects.equals(this.attemptScoring, quizUpdate.attemptScoring) &&
        Objects.equals(this.dropLowestAttempt, quizUpdate.dropLowestAttempt) &&
        Objects.equals(this.drawsFromPool, quizUpdate.drawsFromPool) &&
        Objects.equals(this.showFeedback, quizUpdate.showFeedback) &&
        Objects.equals(this.isInRepository, quizUpdate.isInRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, courseId, lessonId, maxAttempts, passingPercentage, totalMarks, showAnswers, showSubmissionHistory, shuffleQuestions, limitQuestionsTo, durationMinutes, proctoringEnabled, maxViolations, negativeMarkingEnabled, negativeMarks, schedulingEnabled, scheduleStart, scheduleEnd, institutionId, description, isAutoGraded, isAdaptive, equalQuestionWeighting, marksPerQuestion, allowPause, attemptScoring, dropLowestAttempt, drawsFromPool, showFeedback, isInRepository);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizUpdate {\n");
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

