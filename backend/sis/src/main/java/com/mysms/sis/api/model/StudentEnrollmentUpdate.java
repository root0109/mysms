// Owned API contract of sis.
package com.mysms.sis.api.model;

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
 * Fields of StudentEnrollment that can be updated (partial update; see updateMask).
 */

public class StudentEnrollmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rollNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String idCardNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String boardRegistrationNumber;

  /**
   * Placement state.
   */
  public enum EnrollmentStatusEnum {
    ENROLLED("enrolled"),
    
    PROMOTED("promoted"),
    
    DETAINED("detained"),
    
    LEFT("left"),
    
    COMPLETED("completed");

    private final String value;

    EnrollmentStatusEnum(String value) {
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
    public static EnrollmentStatusEnum fromValue(String value) {
      for (EnrollmentStatusEnum b : EnrollmentStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EnrollmentStatusEnum enrollmentStatus;

  /**
   * Year-end result.
   */
  public enum ResultStatusEnum {
    PENDING("pending"),
    
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
  private @Nullable LocalDate enrolledOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate leftOn;

  public StudentEnrollmentUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public StudentEnrollmentUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public StudentEnrollmentUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section.
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

  public StudentEnrollmentUpdate rollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
    return this;
  }

  /**
   * Roll number in the class.
   * @return rollNumber
   */
  @Size(max = 20) 
  @JsonProperty("rollNumber")
  public @Nullable String getRollNumber() {
    return rollNumber;
  }

  @JsonProperty("rollNumber")
  public void setRollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
  }

  public StudentEnrollmentUpdate idCardNumber(@Nullable String idCardNumber) {
    this.idCardNumber = idCardNumber;
    return this;
  }

  /**
   * Identity-card number for the year.
   * @return idCardNumber
   */
  @Size(max = 40) 
  @JsonProperty("idCardNumber")
  public @Nullable String getIdCardNumber() {
    return idCardNumber;
  }

  @JsonProperty("idCardNumber")
  public void setIdCardNumber(@Nullable String idCardNumber) {
    this.idCardNumber = idCardNumber;
  }

  public StudentEnrollmentUpdate boardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
    return this;
  }

  /**
   * Examination-board registration number.
   * @return boardRegistrationNumber
   */
  @Size(max = 40) 
  @JsonProperty("boardRegistrationNumber")
  public @Nullable String getBoardRegistrationNumber() {
    return boardRegistrationNumber;
  }

  @JsonProperty("boardRegistrationNumber")
  public void setBoardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
  }

  public StudentEnrollmentUpdate enrollmentStatus(@Nullable EnrollmentStatusEnum enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  /**
   * Placement state.
   * @return enrollmentStatus
   */
  
  @JsonProperty("enrollmentStatus")
  public @Nullable EnrollmentStatusEnum getEnrollmentStatus() {
    return enrollmentStatus;
  }

  @JsonProperty("enrollmentStatus")
  public void setEnrollmentStatus(@Nullable EnrollmentStatusEnum enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
  }

  public StudentEnrollmentUpdate resultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
    return this;
  }

  /**
   * Year-end result.
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

  public StudentEnrollmentUpdate enrolledOn(@Nullable LocalDate enrolledOn) {
    this.enrolledOn = enrolledOn;
    return this;
  }

  /**
   * Placement date.
   * @return enrolledOn
   */
  @Valid 
  @JsonProperty("enrolledOn")
  public @Nullable LocalDate getEnrolledOn() {
    return enrolledOn;
  }

  @JsonProperty("enrolledOn")
  public void setEnrolledOn(@Nullable LocalDate enrolledOn) {
    this.enrolledOn = enrolledOn;
  }

  public StudentEnrollmentUpdate leftOn(@Nullable LocalDate leftOn) {
    this.leftOn = leftOn;
    return this;
  }

  /**
   * Date the placement ended.
   * @return leftOn
   */
  @Valid 
  @JsonProperty("leftOn")
  public @Nullable LocalDate getLeftOn() {
    return leftOn;
  }

  @JsonProperty("leftOn")
  public void setLeftOn(@Nullable LocalDate leftOn) {
    this.leftOn = leftOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentEnrollmentUpdate studentEnrollmentUpdate = (StudentEnrollmentUpdate) o;
    return Objects.equals(this.studentId, studentEnrollmentUpdate.studentId) &&
        Objects.equals(this.academicYearId, studentEnrollmentUpdate.academicYearId) &&
        Objects.equals(this.classSectionId, studentEnrollmentUpdate.classSectionId) &&
        Objects.equals(this.rollNumber, studentEnrollmentUpdate.rollNumber) &&
        Objects.equals(this.idCardNumber, studentEnrollmentUpdate.idCardNumber) &&
        Objects.equals(this.boardRegistrationNumber, studentEnrollmentUpdate.boardRegistrationNumber) &&
        Objects.equals(this.enrollmentStatus, studentEnrollmentUpdate.enrollmentStatus) &&
        Objects.equals(this.resultStatus, studentEnrollmentUpdate.resultStatus) &&
        Objects.equals(this.enrolledOn, studentEnrollmentUpdate.enrolledOn) &&
        Objects.equals(this.leftOn, studentEnrollmentUpdate.leftOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, academicYearId, classSectionId, rollNumber, idCardNumber, boardRegistrationNumber, enrollmentStatus, resultStatus, enrolledOn, leftOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentEnrollmentUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    rollNumber: ").append(toIndentedString(rollNumber)).append("\n");
    sb.append("    idCardNumber: ").append(toIndentedString(idCardNumber)).append("\n");
    sb.append("    boardRegistrationNumber: ").append(toIndentedString(boardRegistrationNumber)).append("\n");
    sb.append("    enrollmentStatus: ").append(toIndentedString(enrollmentStatus)).append("\n");
    sb.append("    resultStatus: ").append(toIndentedString(resultStatus)).append("\n");
    sb.append("    enrolledOn: ").append(toIndentedString(enrolledOn)).append("\n");
    sb.append("    leftOn: ").append(toIndentedString(leftOn)).append("\n");
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

