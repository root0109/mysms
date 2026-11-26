// Owned API contract of org.
package com.mysms.platform.org.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AcademicTerm that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AcademicTermUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> sequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public AcademicTermUpdate parentTermId(@Nullable Optional<UUID> parentTermId) {
    this.parentTermId = parentTermId;
    return this;
  }

  /**
   * Parent term (e.g. quarter inside a semester).
   * @return parentTermId
   */
  
  @JsonProperty("parentTermId")
  public @Nullable Optional<UUID> getParentTermId() {
    return parentTermId;
  }

  @JsonProperty("parentTermId")
  public void setParentTermId(@Nullable Optional<UUID> parentTermId) {
    this.parentTermId = parentTermId;
  }

  public AcademicTermUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Term name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public AcademicTermUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * First day.
   * @return startDate
   */
  
  @JsonProperty("startDate")
  public @Nullable Optional<LocalDate> getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
  }

  public AcademicTermUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Last day.
   * @return endDate
   */
  
  @JsonProperty("endDate")
  public @Nullable Optional<LocalDate> getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
  }

  public AcademicTermUpdate sequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Order within the year.
   * @return sequence
   */
  
  @JsonProperty("sequence")
  public @Nullable Optional<Integer> getSequence() {
    return sequence;
  }

  @JsonProperty("sequence")
  public void setSequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
  }

  public AcademicTermUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Short code (e.g. FA26).
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public AcademicTermUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Term can be used for new cohorts / registrations.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AcademicTermUpdate academicTermUpdate = (AcademicTermUpdate) o;
    return Objects.equals(this.parentTermId, academicTermUpdate.parentTermId) &&
        Objects.equals(this.name, academicTermUpdate.name) &&
        Objects.equals(this.startDate, academicTermUpdate.startDate) &&
        Objects.equals(this.endDate, academicTermUpdate.endDate) &&
        Objects.equals(this.sequence, academicTermUpdate.sequence) &&
        Objects.equals(this.code, academicTermUpdate.code) &&
        Objects.equals(this.isActive, academicTermUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentTermId, name, startDate, endDate, sequence, code, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AcademicTermUpdate {\n");
    sb.append("    parentTermId: ").append(toIndentedString(parentTermId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    sequence: ").append(toIndentedString(sequence)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

