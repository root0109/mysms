// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * Student&#39;s completion state of a lesson (or SCORM chapter).
 */

public class LessonProgress {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormSuspendData;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormLocation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormScore;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer scormTotalTimeSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  /**
   * State.
   */
  public enum StatusEnum {
    COMPLETE("complete"),
    
    PARTIALLY_COMPLETE("partially_complete"),
    
    INCOMPLETE("incomplete");

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
  private StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isScormChapter;

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

  public LessonProgress() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LessonProgress(UUID userId, UUID courseId, UUID chapterId, StatusEnum status) {
    this.userId = userId;
    this.courseId = courseId;
    this.chapterId = chapterId;
    this.status = status;
  }

  public LessonProgress scormSuspendData(@Nullable String scormSuspendData) {
    this.scormSuspendData = scormSuspendData;
    return this;
  }

  /**
   * SCORM cmi.suspend_data as reported by the package.
   * @return scormSuspendData
   */
  
  @JsonProperty("scormSuspendData")
  public @Nullable String getScormSuspendData() {
    return scormSuspendData;
  }

  @JsonProperty("scormSuspendData")
  public void setScormSuspendData(@Nullable String scormSuspendData) {
    this.scormSuspendData = scormSuspendData;
  }

  public LessonProgress scormLocation(@Nullable String scormLocation) {
    this.scormLocation = scormLocation;
    return this;
  }

  /**
   * SCORM lesson location/bookmark.
   * @return scormLocation
   */
  
  @JsonProperty("scormLocation")
  public @Nullable String getScormLocation() {
    return scormLocation;
  }

  @JsonProperty("scormLocation")
  public void setScormLocation(@Nullable String scormLocation) {
    this.scormLocation = scormLocation;
  }

  public LessonProgress scormScore(@Nullable String scormScore) {
    this.scormScore = scormScore;
    return this;
  }

  /**
   * SCORM reported score.
   * @return scormScore
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("scormScore")
  public @Nullable String getScormScore() {
    return scormScore;
  }

  @JsonProperty("scormScore")
  public void setScormScore(@Nullable String scormScore) {
    this.scormScore = scormScore;
  }

  public LessonProgress scormTotalTimeSeconds(@Nullable Integer scormTotalTimeSeconds) {
    this.scormTotalTimeSeconds = scormTotalTimeSeconds;
    return this;
  }

  /**
   * SCORM total time, in seconds.
   * @return scormTotalTimeSeconds
   */
  
  @JsonProperty("scormTotalTimeSeconds")
  public @Nullable Integer getScormTotalTimeSeconds() {
    return scormTotalTimeSeconds;
  }

  @JsonProperty("scormTotalTimeSeconds")
  public void setScormTotalTimeSeconds(@Nullable Integer scormTotalTimeSeconds) {
    this.scormTotalTimeSeconds = scormTotalTimeSeconds;
  }

  public LessonProgress id(@Nullable UUID id) {
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

  public LessonProgress userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public LessonProgress courseId(UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  @NotNull @Valid 
  @JsonProperty("courseId")
  public UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  public LessonProgress chapterId(UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter.
   * @return chapterId
   */
  @NotNull @Valid 
  @JsonProperty("chapterId")
  public UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(UUID chapterId) {
    this.chapterId = chapterId;
  }

  public LessonProgress lessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson (NULL for SCORM chapter tracking).
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

  public LessonProgress status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public LessonProgress isScormChapter(@Nullable Boolean isScormChapter) {
    this.isScormChapter = isScormChapter;
    return this;
  }

  /**
   * Tracks a SCORM chapter.
   * @return isScormChapter
   */
  
  @JsonProperty("isScormChapter")
  public @Nullable Boolean getIsScormChapter() {
    return isScormChapter;
  }

  @JsonProperty("isScormChapter")
  public void setIsScormChapter(@Nullable Boolean isScormChapter) {
    this.isScormChapter = isScormChapter;
  }

  public LessonProgress createTime(@Nullable OffsetDateTime createTime) {
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

  public LessonProgress createdBy(@Nullable UUID createdBy) {
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

  public LessonProgress updateTime(@Nullable OffsetDateTime updateTime) {
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

  public LessonProgress updatedBy(@Nullable UUID updatedBy) {
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

  public LessonProgress etag(@Nullable String etag) {
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
    LessonProgress lessonProgress = (LessonProgress) o;
    return Objects.equals(this.scormSuspendData, lessonProgress.scormSuspendData) &&
        Objects.equals(this.scormLocation, lessonProgress.scormLocation) &&
        Objects.equals(this.scormScore, lessonProgress.scormScore) &&
        Objects.equals(this.scormTotalTimeSeconds, lessonProgress.scormTotalTimeSeconds) &&
        Objects.equals(this.id, lessonProgress.id) &&
        Objects.equals(this.userId, lessonProgress.userId) &&
        Objects.equals(this.courseId, lessonProgress.courseId) &&
        Objects.equals(this.chapterId, lessonProgress.chapterId) &&
        Objects.equals(this.lessonId, lessonProgress.lessonId) &&
        Objects.equals(this.status, lessonProgress.status) &&
        Objects.equals(this.isScormChapter, lessonProgress.isScormChapter) &&
        Objects.equals(this.createTime, lessonProgress.createTime) &&
        Objects.equals(this.createdBy, lessonProgress.createdBy) &&
        Objects.equals(this.updateTime, lessonProgress.updateTime) &&
        Objects.equals(this.updatedBy, lessonProgress.updatedBy) &&
        Objects.equals(this.etag, lessonProgress.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scormSuspendData, scormLocation, scormScore, scormTotalTimeSeconds, id, userId, courseId, chapterId, lessonId, status, isScormChapter, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonProgress {\n");
    sb.append("    scormSuspendData: ").append(toIndentedString(scormSuspendData)).append("\n");
    sb.append("    scormLocation: ").append(toIndentedString(scormLocation)).append("\n");
    sb.append("    scormScore: ").append(toIndentedString(scormScore)).append("\n");
    sb.append("    scormTotalTimeSeconds: ").append(toIndentedString(scormTotalTimeSeconds)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    isScormChapter: ").append(toIndentedString(isScormChapter)).append("\n");
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

