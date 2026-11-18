// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GenerateFeeDuesRequest
 */

public class GenerateFeeDuesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID feePlanId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> studentIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> programEnrollmentIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String totalAmount;

  public GenerateFeeDuesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GenerateFeeDuesRequest(UUID feePlanId, UUID academicYearId, LocalDate startDate) {
    this.feePlanId = feePlanId;
    this.academicYearId = academicYearId;
    this.startDate = startDate;
  }

  public GenerateFeeDuesRequest feePlanId(UUID feePlanId) {
    this.feePlanId = feePlanId;
    return this;
  }

  /**
   * Fee plan.
   * @return feePlanId
   */
  @NotNull @Valid 
  @JsonProperty("feePlanId")
  public UUID getFeePlanId() {
    return feePlanId;
  }

  @JsonProperty("feePlanId")
  public void setFeePlanId(UUID feePlanId) {
    this.feePlanId = feePlanId;
  }

  public GenerateFeeDuesRequest academicYearId(UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year the dues belong to (part of the duplicate-due guard).
   * @return academicYearId
   */
  @NotNull @Valid 
  @JsonProperty("academicYearId")
  public UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public GenerateFeeDuesRequest studentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
    return this;
  }

  public GenerateFeeDuesRequest addStudentIdsItem(UUID studentIdsItem) {
    if (this.studentIds == null) {
      this.studentIds = new ArrayList<>();
    }
    this.studentIds.add(studentIdsItem);
    return this;
  }

  /**
   * Students.
   * @return studentIds
   */
  
  @JsonProperty("studentIds")
  public List<UUID> getStudentIds() {
    return studentIds;
  }

  @JsonProperty("studentIds")
  public void setStudentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
  }

  public GenerateFeeDuesRequest classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Whole class.
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public GenerateFeeDuesRequest programEnrollmentIds(List<UUID> programEnrollmentIds) {
    this.programEnrollmentIds = programEnrollmentIds;
    return this;
  }

  public GenerateFeeDuesRequest addProgramEnrollmentIdsItem(UUID programEnrollmentIdsItem) {
    if (this.programEnrollmentIds == null) {
      this.programEnrollmentIds = new ArrayList<>();
    }
    this.programEnrollmentIds.add(programEnrollmentIdsItem);
    return this;
  }

  /**
   * Registrations.
   * @return programEnrollmentIds
   */
  
  @JsonProperty("programEnrollmentIds")
  public List<UUID> getProgramEnrollmentIds() {
    return programEnrollmentIds;
  }

  @JsonProperty("programEnrollmentIds")
  public void setProgramEnrollmentIds(List<UUID> programEnrollmentIds) {
    this.programEnrollmentIds = programEnrollmentIds;
  }

  public GenerateFeeDuesRequest startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Schedule start.
   * @return startDate
   */
  @NotNull @Valid 
  @JsonProperty("startDate")
  public LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public GenerateFeeDuesRequest totalAmount(@Nullable String totalAmount) {
    this.totalAmount = totalAmount;
    return this;
  }

  /**
   * Override the sum of fee heads.
   * @return totalAmount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalAmount")
  public @Nullable String getTotalAmount() {
    return totalAmount;
  }

  @JsonProperty("totalAmount")
  public void setTotalAmount(@Nullable String totalAmount) {
    this.totalAmount = totalAmount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GenerateFeeDuesRequest generateFeeDuesRequest = (GenerateFeeDuesRequest) o;
    return Objects.equals(this.feePlanId, generateFeeDuesRequest.feePlanId) &&
        Objects.equals(this.academicYearId, generateFeeDuesRequest.academicYearId) &&
        Objects.equals(this.studentIds, generateFeeDuesRequest.studentIds) &&
        Objects.equals(this.classSectionId, generateFeeDuesRequest.classSectionId) &&
        Objects.equals(this.programEnrollmentIds, generateFeeDuesRequest.programEnrollmentIds) &&
        Objects.equals(this.startDate, generateFeeDuesRequest.startDate) &&
        Objects.equals(this.totalAmount, generateFeeDuesRequest.totalAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feePlanId, academicYearId, studentIds, classSectionId, programEnrollmentIds, startDate, totalAmount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GenerateFeeDuesRequest {\n");
    sb.append("    feePlanId: ").append(toIndentedString(feePlanId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    studentIds: ").append(toIndentedString(studentIds)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    programEnrollmentIds: ").append(toIndentedString(programEnrollmentIds)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    totalAmount: ").append(toIndentedString(totalAmount)).append("\n");
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

