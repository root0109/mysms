// Owned API contract of org.
package com.mysms.platform.org.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AcademicYear that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AcademicYearUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> endDate;

  /**
   * How the year is divided into terms.
   */
  public enum TermStructureEnum {
    TWO_SEMESTERS("two_semesters"),
    
    TWO_SEMESTERS_QUARTERLY("two_semesters_quarterly"),
    
    TWO_SEMESTERS_FINAL("two_semesters_final"),
    
    THREE_SEMESTERS("three_semesters"),
    
    FOUR_QUARTERS("four_quarters"),
    
    FINAL_YEAR("final_year"),
    
    OTHER("other");

    private final String value;

    TermStructureEnum(String value) {
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
    public static TermStructureEnum fromValue(String value) {
      for (TermStructureEnum b : TermStructureEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<TermStructureEnum> termStructure;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCurrent;

  public AcademicYearUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
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

  public AcademicYearUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Year label, e.g. 2026-27.
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

  public AcademicYearUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Notes.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public AcademicYearUpdate startDate(@Nullable Optional<LocalDate> startDate) {
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

  public AcademicYearUpdate endDate(@Nullable Optional<LocalDate> endDate) {
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

  public AcademicYearUpdate termStructure(@Nullable Optional<TermStructureEnum> termStructure) {
    this.termStructure = termStructure;
    return this;
  }

  /**
   * How the year is divided into terms.
   * @return termStructure
   */
  
  @JsonProperty("termStructure")
  public @Nullable Optional<TermStructureEnum> getTermStructure() {
    return termStructure;
  }

  @JsonProperty("termStructure")
  public void setTermStructure(@Nullable Optional<TermStructureEnum> termStructure) {
    this.termStructure = termStructure;
  }

  public AcademicYearUpdate isCurrent(@Nullable Optional<Boolean> isCurrent) {
    this.isCurrent = isCurrent;
    return this;
  }

  /**
   * Current year for the institution (at most one).
   * @return isCurrent
   */
  
  @JsonProperty("isCurrent")
  public @Nullable Optional<Boolean> getIsCurrent() {
    return isCurrent;
  }

  @JsonProperty("isCurrent")
  public void setIsCurrent(@Nullable Optional<Boolean> isCurrent) {
    this.isCurrent = isCurrent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AcademicYearUpdate academicYearUpdate = (AcademicYearUpdate) o;
    return Objects.equals(this.institutionId, academicYearUpdate.institutionId) &&
        Objects.equals(this.name, academicYearUpdate.name) &&
        Objects.equals(this.description, academicYearUpdate.description) &&
        Objects.equals(this.startDate, academicYearUpdate.startDate) &&
        Objects.equals(this.endDate, academicYearUpdate.endDate) &&
        Objects.equals(this.termStructure, academicYearUpdate.termStructure) &&
        Objects.equals(this.isCurrent, academicYearUpdate.isCurrent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, name, description, startDate, endDate, termStructure, isCurrent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AcademicYearUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    termStructure: ").append(toIndentedString(termStructure)).append("\n");
    sb.append("    isCurrent: ").append(toIndentedString(isCurrent)).append("\n");
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

