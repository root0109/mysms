// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

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
 * One student&#39;s aggregate on a marksheet.
 */

public class MarksheetLine {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String creditsAttempted;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String creditsEarned;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradePoints;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sgpa;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String cgpa;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID marksheetId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String totalMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String percentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradeLabel;

  /**
   * Result.
   */
  public enum ResultStatusEnum {
    PASS("pass"),
    
    FAIL("fail");

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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate generatedOn;

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

  public MarksheetLine() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarksheetLine(UUID marksheetId, UUID studentId, LocalDate generatedOn) {
    this.marksheetId = marksheetId;
    this.studentId = studentId;
    this.generatedOn = generatedOn;
  }

  public MarksheetLine creditsAttempted(@Nullable String creditsAttempted) {
    this.creditsAttempted = creditsAttempted;
    return this;
  }

  /**
   * Credits attempted in the period (higher-ed).
   * @return creditsAttempted
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("creditsAttempted")
  public @Nullable String getCreditsAttempted() {
    return creditsAttempted;
  }

  @JsonProperty("creditsAttempted")
  public void setCreditsAttempted(@Nullable String creditsAttempted) {
    this.creditsAttempted = creditsAttempted;
  }

  public MarksheetLine creditsEarned(@Nullable String creditsEarned) {
    this.creditsEarned = creditsEarned;
    return this;
  }

  /**
   * Credits earned in the period (higher-ed).
   * @return creditsEarned
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("creditsEarned")
  public @Nullable String getCreditsEarned() {
    return creditsEarned;
  }

  @JsonProperty("creditsEarned")
  public void setCreditsEarned(@Nullable String creditsEarned) {
    this.creditsEarned = creditsEarned;
  }

  public MarksheetLine gradePoints(@Nullable String gradePoints) {
    this.gradePoints = gradePoints;
    return this;
  }

  /**
   * Total weighted grade points in the period (higher-ed).
   * @return gradePoints
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("gradePoints")
  public @Nullable String getGradePoints() {
    return gradePoints;
  }

  @JsonProperty("gradePoints")
  public void setGradePoints(@Nullable String gradePoints) {
    this.gradePoints = gradePoints;
  }

  public MarksheetLine sgpa(@Nullable String sgpa) {
    this.sgpa = sgpa;
    return this;
  }

  /**
   * Term GPA (higher-ed).
   * @return sgpa
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("sgpa")
  public @Nullable String getSgpa() {
    return sgpa;
  }

  @JsonProperty("sgpa")
  public void setSgpa(@Nullable String sgpa) {
    this.sgpa = sgpa;
  }

  public MarksheetLine cgpa(@Nullable String cgpa) {
    this.cgpa = cgpa;
    return this;
  }

  /**
   * Cumulative GPA to date (higher-ed).
   * @return cgpa
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("cgpa")
  public @Nullable String getCgpa() {
    return cgpa;
  }

  @JsonProperty("cgpa")
  public void setCgpa(@Nullable String cgpa) {
    this.cgpa = cgpa;
  }

  public MarksheetLine id(@Nullable UUID id) {
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

  public MarksheetLine marksheetId(UUID marksheetId) {
    this.marksheetId = marksheetId;
    return this;
  }

  /**
   * Marksheet.
   * @return marksheetId
   */
  @NotNull @Valid 
  @JsonProperty("marksheetId")
  public UUID getMarksheetId() {
    return marksheetId;
  }

  @JsonProperty("marksheetId")
  public void setMarksheetId(UUID marksheetId) {
    this.marksheetId = marksheetId;
  }

  public MarksheetLine studentId(UUID studentId) {
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

  public MarksheetLine totalMarks(@Nullable String totalMarks) {
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

  public MarksheetLine percentage(@Nullable String percentage) {
    this.percentage = percentage;
    return this;
  }

  /**
   * Percentage.
   * @return percentage
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("percentage")
  public @Nullable String getPercentage() {
    return percentage;
  }

  @JsonProperty("percentage")
  public void setPercentage(@Nullable String percentage) {
    this.percentage = percentage;
  }

  public MarksheetLine gradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
    return this;
  }

  /**
   * Grade.
   * @return gradeLabel
   */
  @Size(max = 30) 
  @JsonProperty("gradeLabel")
  public @Nullable String getGradeLabel() {
    return gradeLabel;
  }

  @JsonProperty("gradeLabel")
  public void setGradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
  }

  public MarksheetLine resultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
    return this;
  }

  /**
   * Result.
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

  public MarksheetLine generatedOn(LocalDate generatedOn) {
    this.generatedOn = generatedOn;
    return this;
  }

  /**
   * Generation date.
   * @return generatedOn
   */
  @NotNull @Valid 
  @JsonProperty("generatedOn")
  public LocalDate getGeneratedOn() {
    return generatedOn;
  }

  @JsonProperty("generatedOn")
  public void setGeneratedOn(LocalDate generatedOn) {
    this.generatedOn = generatedOn;
  }

  public MarksheetLine createTime(@Nullable OffsetDateTime createTime) {
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

  public MarksheetLine createdBy(@Nullable UUID createdBy) {
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

  public MarksheetLine updateTime(@Nullable OffsetDateTime updateTime) {
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

  public MarksheetLine updatedBy(@Nullable UUID updatedBy) {
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

  public MarksheetLine etag(@Nullable String etag) {
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
    MarksheetLine marksheetLine = (MarksheetLine) o;
    return Objects.equals(this.creditsAttempted, marksheetLine.creditsAttempted) &&
        Objects.equals(this.creditsEarned, marksheetLine.creditsEarned) &&
        Objects.equals(this.gradePoints, marksheetLine.gradePoints) &&
        Objects.equals(this.sgpa, marksheetLine.sgpa) &&
        Objects.equals(this.cgpa, marksheetLine.cgpa) &&
        Objects.equals(this.id, marksheetLine.id) &&
        Objects.equals(this.marksheetId, marksheetLine.marksheetId) &&
        Objects.equals(this.studentId, marksheetLine.studentId) &&
        Objects.equals(this.totalMarks, marksheetLine.totalMarks) &&
        Objects.equals(this.percentage, marksheetLine.percentage) &&
        Objects.equals(this.gradeLabel, marksheetLine.gradeLabel) &&
        Objects.equals(this.resultStatus, marksheetLine.resultStatus) &&
        Objects.equals(this.generatedOn, marksheetLine.generatedOn) &&
        Objects.equals(this.createTime, marksheetLine.createTime) &&
        Objects.equals(this.createdBy, marksheetLine.createdBy) &&
        Objects.equals(this.updateTime, marksheetLine.updateTime) &&
        Objects.equals(this.updatedBy, marksheetLine.updatedBy) &&
        Objects.equals(this.etag, marksheetLine.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(creditsAttempted, creditsEarned, gradePoints, sgpa, cgpa, id, marksheetId, studentId, totalMarks, percentage, gradeLabel, resultStatus, generatedOn, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarksheetLine {\n");
    sb.append("    creditsAttempted: ").append(toIndentedString(creditsAttempted)).append("\n");
    sb.append("    creditsEarned: ").append(toIndentedString(creditsEarned)).append("\n");
    sb.append("    gradePoints: ").append(toIndentedString(gradePoints)).append("\n");
    sb.append("    sgpa: ").append(toIndentedString(sgpa)).append("\n");
    sb.append("    cgpa: ").append(toIndentedString(cgpa)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    marksheetId: ").append(toIndentedString(marksheetId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    totalMarks: ").append(toIndentedString(totalMarks)).append("\n");
    sb.append("    percentage: ").append(toIndentedString(percentage)).append("\n");
    sb.append("    gradeLabel: ").append(toIndentedString(gradeLabel)).append("\n");
    sb.append("    resultStatus: ").append(toIndentedString(resultStatus)).append("\n");
    sb.append("    generatedOn: ").append(toIndentedString(generatedOn)).append("\n");
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

