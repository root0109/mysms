// Owned API contract of people.
package com.mysms.people.api.model;

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
 * Fields of EducationHistory that can be updated (partial update; see updateMask).
 */

public class EducationHistoryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID qualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String institutionName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String degreeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String major;

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
  private @Nullable GradeTypeEnum gradeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String grade;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String certification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String minor;

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
  private @Nullable StudyModeEnum studyMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public EducationHistoryUpdate qualificationId(@Nullable UUID qualificationId) {
    this.qualificationId = qualificationId;
    return this;
  }

  /**
   * Qualification obtained (when in the catalogue).
   * @return qualificationId
   */
  @Valid 
  @JsonProperty("qualificationId")
  public @Nullable UUID getQualificationId() {
    return qualificationId;
  }

  @JsonProperty("qualificationId")
  public void setQualificationId(@Nullable UUID qualificationId) {
    this.qualificationId = qualificationId;
  }

  public EducationHistoryUpdate institutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
    return this;
  }

  /**
   * School / college / university.
   * @return institutionName
   */
  @Size(max = 200) 
  @JsonProperty("institutionName")
  public @Nullable String getInstitutionName() {
    return institutionName;
  }

  @JsonProperty("institutionName")
  public void setInstitutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
  }

  public EducationHistoryUpdate location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Location of the institution.
   * @return location
   */
  @Size(max = 150) 
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public EducationHistoryUpdate degreeType(@Nullable String degreeType) {
    this.degreeType = degreeType;
    return this;
  }

  /**
   * Degree / certificate type.
   * @return degreeType
   */
  @Size(max = 120) 
  @JsonProperty("degreeType")
  public @Nullable String getDegreeType() {
    return degreeType;
  }

  @JsonProperty("degreeType")
  public void setDegreeType(@Nullable String degreeType) {
    this.degreeType = degreeType;
  }

  public EducationHistoryUpdate major(@Nullable String major) {
    this.major = major;
    return this;
  }

  /**
   * Major / specialisation.
   * @return major
   */
  @Size(max = 120) 
  @JsonProperty("major")
  public @Nullable String getMajor() {
    return major;
  }

  @JsonProperty("major")
  public void setMajor(@Nullable String major) {
    this.major = major;
  }

  public EducationHistoryUpdate gradeType(@Nullable GradeTypeEnum gradeType) {
    this.gradeType = gradeType;
    return this;
  }

  /**
   * Grading system of the grade.
   * @return gradeType
   */
  
  @JsonProperty("gradeType")
  public @Nullable GradeTypeEnum getGradeType() {
    return gradeType;
  }

  @JsonProperty("gradeType")
  public void setGradeType(@Nullable GradeTypeEnum gradeType) {
    this.gradeType = gradeType;
  }

  public EducationHistoryUpdate grade(@Nullable String grade) {
    this.grade = grade;
    return this;
  }

  /**
   * Grade obtained.
   * @return grade
   */
  @Size(max = 30) 
  @JsonProperty("grade")
  public @Nullable String getGrade() {
    return grade;
  }

  @JsonProperty("grade")
  public void setGrade(@Nullable String grade) {
    this.grade = grade;
  }

  public EducationHistoryUpdate startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Start date.
   * @return startDate
   */
  @Valid 
  @JsonProperty("startDate")
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
  }

  public EducationHistoryUpdate endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * End date.
   * @return endDate
   */
  @Valid 
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public EducationHistoryUpdate certification(@Nullable String certification) {
    this.certification = certification;
    return this;
  }

  /**
   * Certificate / degree title obtained.
   * @return certification
   */
  @Size(max = 200) 
  @JsonProperty("certification")
  public @Nullable String getCertification() {
    return certification;
  }

  @JsonProperty("certification")
  public void setCertification(@Nullable String certification) {
    this.certification = certification;
  }

  public EducationHistoryUpdate minor(@Nullable String minor) {
    this.minor = minor;
    return this;
  }

  /**
   * Minor subject.
   * @return minor
   */
  @Size(max = 200) 
  @JsonProperty("minor")
  public @Nullable String getMinor() {
    return minor;
  }

  @JsonProperty("minor")
  public void setMinor(@Nullable String minor) {
    this.minor = minor;
  }

  public EducationHistoryUpdate studyMode(@Nullable StudyModeEnum studyMode) {
    this.studyMode = studyMode;
    return this;
  }

  /**
   * Mode of study.
   * @return studyMode
   */
  
  @JsonProperty("studyMode")
  public @Nullable StudyModeEnum getStudyMode() {
    return studyMode;
  }

  @JsonProperty("studyMode")
  public void setStudyMode(@Nullable StudyModeEnum studyMode) {
    this.studyMode = studyMode;
  }

  public EducationHistoryUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
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

