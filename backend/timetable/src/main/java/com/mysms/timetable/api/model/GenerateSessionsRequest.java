// Owned API contract of timetable.
package com.mysms.timetable.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.timetable.api.model.WeeklySlot;
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
 * GenerateSessionsRequest
 */

public class GenerateSessionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid WeeklySlot> slots = new ArrayList<>();

  public GenerateSessionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GenerateSessionsRequest(UUID institutionId, LocalDate startDate, LocalDate endDate, List<@Valid WeeklySlot> slots) {
    this.institutionId = institutionId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.slots = slots;
  }

  public GenerateSessionsRequest institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public GenerateSessionsRequest classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (K-12).
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

  public GenerateSessionsRequest courseId(@Nullable UUID courseId) {
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

  public GenerateSessionsRequest batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch.
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public GenerateSessionsRequest startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * From.
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

  public GenerateSessionsRequest endDate(LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * To.
   * @return endDate
   */
  @NotNull @Valid 
  @JsonProperty("endDate")
  public LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public GenerateSessionsRequest slots(List<@Valid WeeklySlot> slots) {
    this.slots = slots;
    return this;
  }

  public GenerateSessionsRequest addSlotsItem(WeeklySlot slotsItem) {
    if (this.slots == null) {
      this.slots = new ArrayList<>();
    }
    this.slots.add(slotsItem);
    return this;
  }

  /**
   * Weekly pattern.
   * @return slots
   */
  @NotNull 
  @JsonProperty("slots")
  public List<@Valid WeeklySlot> getSlots() {
    return slots;
  }

  @JsonProperty("slots")
  public void setSlots(List<@Valid WeeklySlot> slots) {
    this.slots = slots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GenerateSessionsRequest generateSessionsRequest = (GenerateSessionsRequest) o;
    return Objects.equals(this.institutionId, generateSessionsRequest.institutionId) &&
        Objects.equals(this.classSectionId, generateSessionsRequest.classSectionId) &&
        Objects.equals(this.courseId, generateSessionsRequest.courseId) &&
        Objects.equals(this.batchId, generateSessionsRequest.batchId) &&
        Objects.equals(this.startDate, generateSessionsRequest.startDate) &&
        Objects.equals(this.endDate, generateSessionsRequest.endDate) &&
        Objects.equals(this.slots, generateSessionsRequest.slots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, classSectionId, courseId, batchId, startDate, endDate, slots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GenerateSessionsRequest {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    slots: ").append(toIndentedString(slots)).append("\n");
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

