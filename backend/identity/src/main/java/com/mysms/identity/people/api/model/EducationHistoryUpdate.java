// Owned API contract of people.
package com.mysms.identity.people.api.model;

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
 * Fields of EducationHistory that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EducationHistoryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> qualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> institutionName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> degreeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> major;

  /**
   * Grading system of the grade.
   */
  public enum GradeTypeEnum {
    PERCENTAGE("percentage"),
    
    SCORE("score"),
    
    LETTER_GRADE("letter_grade"),
    
    UK_CLASS("uk_class"),
    
    FRENCH_MENTION("french_mention"),
    
    CGPA_4("cgpa_4"),
    
    CGPA_10("cgpa_10");

    private final String value;

    GradeTypeEnum(String value) {
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
    public static GradeTypeEnum fromValue(String value) {
      for (GradeTypeEnum b : GradeTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<GradeTypeEnum> gradeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> grade;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> certification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> minor;

  /**
   * Mode of study.
   */
  public enum StudyModeEnum {
    FULL_TIME("full_time"),
    
    PART_TIME("part_time"),
    
    CORRESPONDENCE("correspondence"),
    
    ONLINE("online");

    private final String value;

    StudyModeEnum(String value) {
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
    public static StudyModeEnum fromValue(String value) {
      for (StudyModeEnum b : StudyModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<StudyModeEnum> studyMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public EducationHistoryUpdate qualificationId(@Nullable Optional<UUID> qualificationId) {
    this.qualificationId = qualificationId;
    return this;
  }

  /**
   * Qualification obtained (when in the catalogue).
   * @return qualificationId
   */
  
  @JsonProperty("qualificationId")
  public @Nullable Optional<UUID> getQualificationId() {
    return qualificationId;
  }

  @JsonProperty("qualificationId")
  public void setQualificationId(@Nullable Optional<UUID> qualificationId) {
    this.qualificationId = qualificationId;
  }

  public EducationHistoryUpdate institutionName(@Nullable Optional<String> institutionName) {
    this.institutionName = institutionName;
    return this;
  }

  /**
   * School / college / university.
   * @return institutionName
   */
  
  @JsonProperty("institutionName")
  public @Nullable Optional<String> getInstitutionName() {
    return institutionName;
  }

  @JsonProperty("institutionName")
  public void setInstitutionName(@Nullable Optional<String> institutionName) {
    this.institutionName = institutionName;
  }

  public EducationHistoryUpdate location(@Nullable Optional<String> location) {
    this.location = location;
    return this;
  }

  /**
   * Location of the institution.
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable Optional<String> getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable Optional<String> location) {
    this.location = location;
  }

  public EducationHistoryUpdate degreeType(@Nullable Optional<String> degreeType) {
    this.degreeType = degreeType;
    return this;
  }

  /**
   * Degree / certificate type.
   * @return degreeType
   */
  
  @JsonProperty("degreeType")
  public @Nullable Optional<String> getDegreeType() {
    return degreeType;
  }

  @JsonProperty("degreeType")
  public void setDegreeType(@Nullable Optional<String> degreeType) {
    this.degreeType = degreeType;
  }

  public EducationHistoryUpdate major(@Nullable Optional<String> major) {
    this.major = major;
    return this;
  }

  /**
   * Major / specialisation.
   * @return major
   */
  
  @JsonProperty("major")
  public @Nullable Optional<String> getMajor() {
    return major;
  }

  @JsonProperty("major")
  public void setMajor(@Nullable Optional<String> major) {
    this.major = major;
  }

  public EducationHistoryUpdate gradeType(@Nullable Optional<GradeTypeEnum> gradeType) {
    this.gradeType = gradeType;
    return this;
  }

  /**
   * Grading system of the grade.
   * @return gradeType
   */
  
  @JsonProperty("gradeType")
  public @Nullable Optional<GradeTypeEnum> getGradeType() {
    return gradeType;
  }

  @JsonProperty("gradeType")
  public void setGradeType(@Nullable Optional<GradeTypeEnum> gradeType) {
    this.gradeType = gradeType;
  }

  public EducationHistoryUpdate grade(@Nullable Optional<String> grade) {
    this.grade = grade;
    return this;
  }

  /**
   * Grade obtained.
   * @return grade
   */
  
  @JsonProperty("grade")
  public @Nullable Optional<String> getGrade() {
    return grade;
  }

  @JsonProperty("grade")
  public void setGrade(@Nullable Optional<String> grade) {
    this.grade = grade;
  }

  public EducationHistoryUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Start date.
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

  public EducationHistoryUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * End date.
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

  public EducationHistoryUpdate certification(@Nullable Optional<String> certification) {
    this.certification = certification;
    return this;
  }

  /**
   * Certificate / degree title obtained.
   * @return certification
   */
  
  @JsonProperty("certification")
  public @Nullable Optional<String> getCertification() {
    return certification;
  }

  @JsonProperty("certification")
  public void setCertification(@Nullable Optional<String> certification) {
    this.certification = certification;
  }

  public EducationHistoryUpdate minor(@Nullable Optional<String> minor) {
    this.minor = minor;
    return this;
  }

  /**
   * Minor subject.
   * @return minor
   */
  
  @JsonProperty("minor")
  public @Nullable Optional<String> getMinor() {
    return minor;
  }

  @JsonProperty("minor")
  public void setMinor(@Nullable Optional<String> minor) {
    this.minor = minor;
  }

  public EducationHistoryUpdate studyMode(@Nullable Optional<StudyModeEnum> studyMode) {
    this.studyMode = studyMode;
    return this;
  }

  /**
   * Mode of study.
   * @return studyMode
   */
  
  @JsonProperty("studyMode")
  public @Nullable Optional<StudyModeEnum> getStudyMode() {
    return studyMode;
  }

  @JsonProperty("studyMode")
  public void setStudyMode(@Nullable Optional<StudyModeEnum> studyMode) {
    this.studyMode = studyMode;
  }

  public EducationHistoryUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EducationHistoryUpdate educationHistoryUpdate = (EducationHistoryUpdate) o;
    return Objects.equals(this.qualificationId, educationHistoryUpdate.qualificationId) &&
        Objects.equals(this.institutionName, educationHistoryUpdate.institutionName) &&
        Objects.equals(this.location, educationHistoryUpdate.location) &&
        Objects.equals(this.degreeType, educationHistoryUpdate.degreeType) &&
        Objects.equals(this.major, educationHistoryUpdate.major) &&
        Objects.equals(this.gradeType, educationHistoryUpdate.gradeType) &&
        Objects.equals(this.grade, educationHistoryUpdate.grade) &&
        Objects.equals(this.startDate, educationHistoryUpdate.startDate) &&
        Objects.equals(this.endDate, educationHistoryUpdate.endDate) &&
        Objects.equals(this.certification, educationHistoryUpdate.certification) &&
        Objects.equals(this.minor, educationHistoryUpdate.minor) &&
        Objects.equals(this.studyMode, educationHistoryUpdate.studyMode) &&
        Objects.equals(this.sortOrder, educationHistoryUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualificationId, institutionName, location, degreeType, major, gradeType, grade, startDate, endDate, certification, minor, studyMode, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EducationHistoryUpdate {\n");
    sb.append("    qualificationId: ").append(toIndentedString(qualificationId)).append("\n");
    sb.append("    institutionName: ").append(toIndentedString(institutionName)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    degreeType: ").append(toIndentedString(degreeType)).append("\n");
    sb.append("    major: ").append(toIndentedString(major)).append("\n");
    sb.append("    gradeType: ").append(toIndentedString(gradeType)).append("\n");
    sb.append("    grade: ").append(toIndentedString(grade)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    certification: ").append(toIndentedString(certification)).append("\n");
    sb.append("    minor: ").append(toIndentedString(minor)).append("\n");
    sb.append("    studyMode: ").append(toIndentedString(studyMode)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

