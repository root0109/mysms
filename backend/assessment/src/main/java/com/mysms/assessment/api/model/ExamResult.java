// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * Student sitting a paper, with room and marks.
 */

public class ExamResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradeLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradePoint;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String credits;

  /**
   * Graded outcome, written by the marks-entry workflow.
   */
  public enum ResultStatusEnum {
    PASS("pass"),
    
    FAIL("fail"),
    
    ABSENT("absent"),
    
    WITHHELD("withheld"),
    
    PENDING("pending");

    private final String value;

    ResultStatusEnum(String value) {
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
    public static ResultStatusEnum fromValue(String value) {
      for (ResultStatusEnum b : ResultStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ResultStatusEnum resultStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID marksheetLineId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evaluatedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime evaluatedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID examId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roomId;

  /**
   * Attendance.
   */
  public enum AttendanceStatusEnum {
    EXPECTED("expected"),
    
    PRESENT("present"),
    
    ABSENT("absent"),
    
    EXEMPTED("exempted");

    private final String value;

    AttendanceStatusEnum(String value) {
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
    public static AttendanceStatusEnum fromValue(String value) {
      for (AttendanceStatusEnum b : AttendanceStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AttendanceStatusEnum attendanceStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

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

  public ExamResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExamResult(UUID examId, UUID studentId) {
    this.examId = examId;
    this.studentId = studentId;
  }

  public ExamResult maxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
    return this;
  }

  /**
   * Maximum marks snapshot at entry time.
   * @return maxMarks
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("maxMarks")
  public @Nullable String getMaxMarks() {
    return maxMarks;
  }

  @JsonProperty("maxMarks")
  public void setMaxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
  }

  public ExamResult gradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
    return this;
  }

  /**
   * Grade awarded from the grading scheme.
   * @return gradeLabel
   */
  
  @JsonProperty("gradeLabel")
  public @Nullable String getGradeLabel() {
    return gradeLabel;
  }

  @JsonProperty("gradeLabel")
  public void setGradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
  }

  public ExamResult gradePoint(@Nullable String gradePoint) {
    this.gradePoint = gradePoint;
    return this;
  }

  /**
   * Grade points for GPA systems.
   * @return gradePoint
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("gradePoint")
  public @Nullable String getGradePoint() {
    return gradePoint;
  }

  @JsonProperty("gradePoint")
  public void setGradePoint(@Nullable String gradePoint) {
    this.gradePoint = gradePoint;
  }

  public ExamResult credits(@Nullable String credits) {
    this.credits = credits;
    return this;
  }

  /**
   * Credits the exam carries (snapshot from the course plan).
   * @return credits
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("credits")
  public @Nullable String getCredits() {
    return credits;
  }

  @JsonProperty("credits")
  public void setCredits(@Nullable String credits) {
    this.credits = credits;
  }

  public ExamResult resultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
    return this;
  }

  /**
   * Graded outcome, written by the marks-entry workflow.
   * @return resultStatus
   */
  
  @JsonProperty("resultStatus")
  public @Nullable ResultStatusEnum getResultStatus() {
    return resultStatus;
  }

  @JsonProperty("resultStatus")
  public void setResultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
  }

  public ExamResult marksheetLineId(@Nullable UUID marksheetLineId) {
    this.marksheetLineId = marksheetLineId;
    return this;
  }

  /**
   * Marksheet line this result was aggregated into.
   * @return marksheetLineId
   */
  @Valid 
  @JsonProperty("marksheetLineId")
  public @Nullable UUID getMarksheetLineId() {
    return marksheetLineId;
  }

  @JsonProperty("marksheetLineId")
  public void setMarksheetLineId(@Nullable UUID marksheetLineId) {
    this.marksheetLineId = marksheetLineId;
  }

  public ExamResult evaluatedByUserId(@Nullable UUID evaluatedByUserId) {
    this.evaluatedByUserId = evaluatedByUserId;
    return this;
  }

  /**
   * User who entered/approved the marks.
   * @return evaluatedByUserId
   */
  @Valid 
  @JsonProperty("evaluatedByUserId")
  public @Nullable UUID getEvaluatedByUserId() {
    return evaluatedByUserId;
  }

  @JsonProperty("evaluatedByUserId")
  public void setEvaluatedByUserId(@Nullable UUID evaluatedByUserId) {
    this.evaluatedByUserId = evaluatedByUserId;
  }

  public ExamResult evaluatedAt(@Nullable OffsetDateTime evaluatedAt) {
    this.evaluatedAt = evaluatedAt;
    return this;
  }

  /**
   * When marks were entered.
   * @return evaluatedAt
   */
  @Valid 
  @JsonProperty("evaluatedAt")
  public @Nullable OffsetDateTime getEvaluatedAt() {
    return evaluatedAt;
  }

  @JsonProperty("evaluatedAt")
  public void setEvaluatedAt(@Nullable OffsetDateTime evaluatedAt) {
    this.evaluatedAt = evaluatedAt;
  }

  public ExamResult id(@Nullable UUID id) {
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

  public ExamResult examId(UUID examId) {
    this.examId = examId;
    return this;
  }

  /**
   * Paper.
   * @return examId
   */
  @NotNull @Valid 
  @JsonProperty("examId")
  public UUID getExamId() {
    return examId;
  }

  @JsonProperty("examId")
  public void setExamId(UUID examId) {
    this.examId = examId;
  }

  public ExamResult studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public ExamResult roomId(@Nullable UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Exam room.
   * @return roomId
   */
  @Valid 
  @JsonProperty("roomId")
  public @Nullable UUID getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(@Nullable UUID roomId) {
    this.roomId = roomId;
  }

  public ExamResult attendanceStatus(@Nullable AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
    return this;
  }

  /**
   * Attendance.
   * @return attendanceStatus
   */
  
  @JsonProperty("attendanceStatus")
  public @Nullable AttendanceStatusEnum getAttendanceStatus() {
    return attendanceStatus;
  }

  @JsonProperty("attendanceStatus")
  public void setAttendanceStatus(@Nullable AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
  }

  public ExamResult marks(@Nullable String marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Marks.
   * @return marks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marks")
  public @Nullable String getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(@Nullable String marks) {
    this.marks = marks;
  }

  public ExamResult note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Note.
   * @return note
   */
  
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  public ExamResult createTime(@Nullable OffsetDateTime createTime) {
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

  public ExamResult createdBy(@Nullable UUID createdBy) {
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

  public ExamResult updateTime(@Nullable OffsetDateTime updateTime) {
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

  public ExamResult updatedBy(@Nullable UUID updatedBy) {
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

  public ExamResult etag(@Nullable String etag) {
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
    ExamResult examResult = (ExamResult) o;
    return Objects.equals(this.maxMarks, examResult.maxMarks) &&
        Objects.equals(this.gradeLabel, examResult.gradeLabel) &&
        Objects.equals(this.gradePoint, examResult.gradePoint) &&
        Objects.equals(this.credits, examResult.credits) &&
        Objects.equals(this.resultStatus, examResult.resultStatus) &&
        Objects.equals(this.marksheetLineId, examResult.marksheetLineId) &&
        Objects.equals(this.evaluatedByUserId, examResult.evaluatedByUserId) &&
        Objects.equals(this.evaluatedAt, examResult.evaluatedAt) &&
        Objects.equals(this.id, examResult.id) &&
        Objects.equals(this.examId, examResult.examId) &&
        Objects.equals(this.studentId, examResult.studentId) &&
        Objects.equals(this.roomId, examResult.roomId) &&
        Objects.equals(this.attendanceStatus, examResult.attendanceStatus) &&
        Objects.equals(this.marks, examResult.marks) &&
        Objects.equals(this.note, examResult.note) &&
        Objects.equals(this.createTime, examResult.createTime) &&
        Objects.equals(this.createdBy, examResult.createdBy) &&
        Objects.equals(this.updateTime, examResult.updateTime) &&
        Objects.equals(this.updatedBy, examResult.updatedBy) &&
        Objects.equals(this.etag, examResult.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxMarks, gradeLabel, gradePoint, credits, resultStatus, marksheetLineId, evaluatedByUserId, evaluatedAt, id, examId, studentId, roomId, attendanceStatus, marks, note, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamResult {\n");
    sb.append("    maxMarks: ").append(toIndentedString(maxMarks)).append("\n");
    sb.append("    gradeLabel: ").append(toIndentedString(gradeLabel)).append("\n");
    sb.append("    gradePoint: ").append(toIndentedString(gradePoint)).append("\n");
    sb.append("    credits: ").append(toIndentedString(credits)).append("\n");
    sb.append("    resultStatus: ").append(toIndentedString(resultStatus)).append("\n");
    sb.append("    marksheetLineId: ").append(toIndentedString(marksheetLineId)).append("\n");
    sb.append("    evaluatedByUserId: ").append(toIndentedString(evaluatedByUserId)).append("\n");
    sb.append("    evaluatedAt: ").append(toIndentedString(evaluatedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    examId: ").append(toIndentedString(examId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    attendanceStatus: ").append(toIndentedString(attendanceStatus)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

